package uk.gov.caz.psr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.PaymentsController;

@FullyRunningServerIntegrationTest
public class PaymentDetailsWithModificationHistoryTestIT {

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static ClientAndServer vccsServiceMockServer;
  private static ClientAndServer accountServiceMockServer;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath =
        PaymentsController.BASE_PATH + "/" + PaymentsController.GET_PAYMENT_DETAILS;
  }

  @BeforeEach
  public void insertTestData() {
    // we cannot use SQL annotations on this class, see:
    // https://github.com/spring-projects/spring-framework/issues/19930
    clearDatabase();
    executeSqlFrom("data/sql/payment-history-by-reference/test-data.sql");
  }

  @AfterEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  @BeforeAll
  public static void initServices() {
    vccsServiceMockServer = startClientAndServer(1090);
    accountServiceMockServer = startClientAndServer(1091);
  }

  @AfterAll
  public static void tearDownServices() {
    vccsServiceMockServer.stop();
    accountServiceMockServer.stop();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private void mockSuccessVccsCleanAirZonesResponse() {
    vccsServiceMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/compliance-checker/clean-air-zones"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-clean-air-zones.json")));
  }

  private void mockSuccessAccountServiceResponse() {
    accountServiceMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/users/60454045-f536-4530-8d2d-22e9b4a6d415"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-account-user-data.json")));
  }

  private PaymentDetailsWithModificationHistoryAssertion whenRequested() {
    return PaymentDetailsWithModificationHistoryAssertion.whenRequested(entityManagerFactory);
  }

  @Nested
  class WhenRequestedForNonExistingPayment {

    @Test
    public void shouldReturnNotFoundResponse() {
      whenRequested()
          .withRandomPaymentId()
          .then()
          .headerContainsCorrelationId()
          .responseHasNotFoundStatus();
    }
  }

  @Nested
  class WhenRequestedForExistingPaymentWithoutChargeback {

    @Test
    public void shouldReturnValidResponse() {
      mockSuccessAccountServiceResponse();
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentIdEqualTo(UUID.fromString("282ccd65-1319-4b3b-a21c-dfe58809bedf"))
          .then()
          .headerContainsCorrelationId()
          .hasEmptyModificationHistory()
          .responseHasOkStatus();
    }
  }

  @Nested
  class WhenRequestedForExistingPaymentWithChargebackByLA {

    @Test
    public void shouldReturnValidResponse() {
      mockSuccessAccountServiceResponse();
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentIdEqualTo(UUID.fromString("391017e8-e2d5-467f-b271-f6cf966eb931"))
          .then()
          .headerContainsCorrelationId()
          .hasElementsInModificationHistory(3)
          .responseHasOkStatus();
    }
  }

  @Nested
  class WhenRequestedForPaymentWithEntrantPaymentTakenFromAnotherPaymentAndChargebackByLA {

    @Test
    public void shouldReturnValidResponse() {
      mockSuccessAccountServiceResponse();
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentIdEqualTo(UUID.fromString("485dc5d0-14e1-4007-997e-c2d3cf8b6d1e"))
          .then()
          .headerContainsCorrelationId()
          .hasElementsInModificationHistory(1)
          .responseHasOkStatus();
    }
  }

  private static class PaymentDetailsWithModificationHistoryAssertion {

    private Statistics statistics;
    private RequestSpecification requestSpecification = commonRequestSpecification();
    private ValidatableResponse validatableResponse;

    public PaymentDetailsWithModificationHistoryAssertion(
        EntityManagerFactory entityManagerFactory) {
      clearHibernateStats(entityManagerFactory);
    }

    private void clearHibernateStats(EntityManagerFactory entityManagerFactory) {
      statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
      statistics.clear();
    }

    private RequestSpecification commonRequestSpecification() {
      return RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
    }

    public static PaymentDetailsWithModificationHistoryAssertion whenRequested(
        EntityManagerFactory entityManagerFactory) {
      return new PaymentDetailsWithModificationHistoryAssertion(entityManagerFactory);
    }

    public PaymentDetailsWithModificationHistoryAssertion withRandomPaymentId() {
      return withPaymentIdEqualTo(UUID.randomUUID());
    }

    public PaymentDetailsWithModificationHistoryAssertion withPaymentIdEqualTo(UUID paymentId) {
      requestSpecification = requestSpecification.pathParam("payment_id", paymentId.toString());
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion then() {
      validatableResponse = requestSpecification.get().then();
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion headerContainsCorrelationId() {
      validatableResponse = validatableResponse
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion responseHasOkStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.OK.value());
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion responseHasNotFoundStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.NOT_FOUND.value());
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion hasEmptyModificationHistory() {
      validatableResponse.body("modificationHistory", is(emptyIterable()));
      return this;
    }

    public PaymentDetailsWithModificationHistoryAssertion hasElementsInModificationHistory(
        int size) {
      validatableResponse.body("modificationHistory", is(iterableWithSize(size)));
      return this;
    }
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
