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
import java.math.BigInteger;
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
public class PaymentHistoryByReferenceTestIT {

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static ClientAndServer vccsServiceMockServer;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Nested
  class WhenRequestedForNonExistingPayment {

    @Test
    public void shouldReturnNotFoundResponse() {
      whenRequested()
          .withRandomPaymentReference()
          .then()
          .headerContainsCorrelationId()
          .responseHasNotFoundStatus();
    }
  }

  @Nested
  class WhenRequestedForExistingPaymentWithoutChargeback {

    @Test
    public void shouldReturnValidResponse() {
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentReferenceEqualTo(BigInteger.valueOf(1881L))
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
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentReferenceEqualTo(BigInteger.valueOf(87L))
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
      mockSuccessVccsCleanAirZonesResponse();

      whenRequested()
          .withPaymentReferenceEqualTo(BigInteger.valueOf(998L))
          .then()
          .headerContainsCorrelationId()
          .hasElementsInModificationHistory(1)
          .responseHasOkStatus();
    }
  }

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath =
        PaymentsController.BASE_PATH + "/" + PaymentsController.GET_REFERENCES_HISTORY;
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
  }

  @AfterAll
  public static void tearDownServices() {
    vccsServiceMockServer.stop();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private PaymentHistoryByReferenceAssertion whenRequested() {
    return PaymentHistoryByReferenceAssertion.whenRequested(entityManagerFactory);
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


  private static class PaymentHistoryByReferenceAssertion {

    private Statistics statistics;
    private RequestSpecification requestSpecification = commonRequestSpecification();
    private ValidatableResponse validatableResponse;

    private PaymentHistoryByReferenceAssertion(EntityManagerFactory entityManagerFactory) {
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


    public static PaymentHistoryByReferenceAssertion whenRequested(
        EntityManagerFactory entityManagerFactory) {
      return new PaymentHistoryByReferenceAssertion(entityManagerFactory);
    }

    public PaymentHistoryByReferenceAssertion withRandomPaymentReference() {
      return withPaymentReferenceEqualTo(BigInteger.valueOf(98789L));
    }

    public PaymentHistoryByReferenceAssertion withPaymentReferenceEqualTo(
        BigInteger paymentReference) {
      requestSpecification = requestSpecification.pathParam("paymentReference", paymentReference);
      return this;
    }

    public PaymentHistoryByReferenceAssertion then() {
      validatableResponse = requestSpecification.get().then();
      return this;
    }

    public PaymentHistoryByReferenceAssertion headerContainsCorrelationId() {
      validatableResponse = validatableResponse.header(Constants.X_CORRELATION_ID_HEADER,
          ANY_CORRELATION_ID);
      return this;
    }

    public PaymentHistoryByReferenceAssertion responseHasOkStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.OK.value());
      return this;
    }

    public PaymentHistoryByReferenceAssertion responseHasNotFoundStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.NOT_FOUND.value());
      return this;
    }

    public PaymentHistoryByReferenceAssertion hasEmptyModificationHistory() {
      validatableResponse.body("modificationHistory", is(emptyIterable()));
      return this;
    }

    public PaymentHistoryByReferenceAssertion hasElementsInModificationHistory(int size) {
      validatableResponse.body("modificationHistory", is(iterableWithSize(size)));
      return this;
    }
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
