package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.caz.psr.controller.PaymentsInfoByOperatorController;

@FullyRunningServerIntegrationTest
public class PaymentInfoByOperatorIdTestIT {

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static ClientAndServer vccsServiceMockServer;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Nested
  class Validation {

    @Nested
    class WhenRequestedWithMalformedOperatorId {

      @ParameterizedTest
      @ValueSource(strings = {"not-uuid-1", "a"})
      public void shouldReturnResponseWith400StatusCode(String operatorId) {
        whenRequested()
            .withOperatorIdEqualTo(operatorId)
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(1)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'operatorId' must be a valid UUID");
      }
    }

    @Nested
    class WhenRequestedWithNegativePageNumber {

      @ParameterizedTest
      @ValueSource(ints = {-1, -2, -10, -15})
      public void shouldReturnResponseWith400StatusCode(int pageNumber) {
        whenRequested()
            .withRandomOperatorId()
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(pageNumber)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'pageNumber' must be non-negative");
      }
    }

    @Nested
    class WhenRequestedWithNonPositivePageSize {

      @ParameterizedTest
      @ValueSource(ints = {0, -1, -2, -10, -15})
      public void shouldReturnResponseWith400StatusCode(int pageSize) {
        whenRequested()
            .withRandomOperatorId()
            .withPageSizeEqualTo(pageSize)
            .withPageNumberEqualTo(0)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'pageSize' must be positive");
      }
    }
  }

  @Nested
  class WhenRequestedForNonExistingOperatorId {

    @Nested
    class WithAnyValidPageNumber {

      @ParameterizedTest
      @ValueSource(ints = {0, 1, 15, 78, 122, 2009})
      public void shouldReturnEmptyResponse(int pageNumber) {
        whenRequested()
            .withRandomOperatorId()
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(pageNumber)
            .then()
            .headerContainsCorrelationId()
            .andPerPageIsEqualTo(5)
            .andPageIsEqualTo(pageNumber)
            .andPageCountIsEqualTo(0)
            .responseHasOkStatus()
            .isEmpty();
      }
    }

    @Nested
    class WithAnyValidPageSize {

      @ParameterizedTest
      @ValueSource(ints = {1, 15, 78, 122, 2020})
      public void shouldReturnEmptyResponse(int pageSize) {
        whenRequested()
            .withRandomOperatorId()
            .withPageSizeEqualTo(pageSize)
            .withPageNumberEqualTo(0)
            .then()
            .headerContainsCorrelationId()
            .responseHasOkStatus()
            .andPerPageIsEqualTo(pageSize)
            .andPageIsEqualTo(0)
            .andPageCountIsEqualTo(0)
            .isEmpty();
      }
    }
  }

  @Nested
  class WhenRequestedForExistingOperatorId {

    @Nested
    class WithDefaultPageSizeAndNumber {

      @Test
      public void shouldReturnPaginatedResultsOnOnePage() {
        mockSuccessVccsCleanAirZonesResponse();
        List<Map<String, Object>> expectedPayments = getExpectedPayments();

        whenRequested()
            .withOperatorIdEqualTo("24f630ec-47c6-4cd0-b8aa-1e05a1463492")
            .then()
            .headerContainsCorrelationId()
            .responseHasOkStatus()
            .andPageIsEqualTo(0)
            .andPageCountIsEqualTo(1)
            .andPerPageIsEqualTo(10)
            .andTotalPaymentsCountIsEqualTo(4)
            .andPaymentsSizeIsEqualTo(4)
            .andPaymentsEqualTo(expectedPayments)
            .andResultsWereFetchedByTwoDatabaseQueries();
      }
    }

    @Test
    public void shouldReturnPaginatedResults() {
      mockSuccessVccsCleanAirZonesResponse();
      List<Map<String, Object>> expectedPayments = getExpectedPayments();

      // first page
      whenRequested()
          .withOperatorIdEqualTo("24f630ec-47c6-4cd0-b8aa-1e05a1463492")
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(0)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(0)
          .andPageCountIsEqualTo(4)
          .andTotalPaymentsCountIsEqualTo(4)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(0)))
          .andResultsWereFetchedByThreeDatabaseQueries();

      // second page
      whenRequested()
          .withOperatorIdEqualTo("24f630ec-47c6-4cd0-b8aa-1e05a1463492")
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(1)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(1)
          .andPageCountIsEqualTo(4)
          .andTotalPaymentsCountIsEqualTo(4)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(1)));

      // third page
      whenRequested()
          .withOperatorIdEqualTo("24f630ec-47c6-4cd0-b8aa-1e05a1463492")
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(2)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(2)
          .andPageCountIsEqualTo(4)
          .andTotalPaymentsCountIsEqualTo(4)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(2)));

      // fourth page
      whenRequested()
          .withOperatorIdEqualTo("24f630ec-47c6-4cd0-b8aa-1e05a1463492")
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(3)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(3)
          .andPageCountIsEqualTo(4)
          .andTotalPaymentsCountIsEqualTo(4)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(3)));
    }

    private List<Map<String, Object>> getExpectedPayments() {
      return Arrays.asList(
          ImmutableMap.<String, Object>builder()
              .put("paymentTimestamp", "2019-11-26 20:39:08")
              .put("totalPaid", 780)
              .put("cazName", "Bath")
              .put("paymentId", "3e06222d-dd81-4621-8915-b2a03a8da9ef")
              .put("paymentReference", 22381)
              .put("paymentProviderStatus", "FAILED")
              .put("vrns", Collections.singletonList("MD16ABC"))
              .put("isChargedback", false)
              .put("isRefunded", false)
              .build(),

          ImmutableMap.<String, Object>builder()
              .put("paymentTimestamp", "2019-11-25 20:39:08")
              .put("totalPaid", 280)
              .put("cazName", "Leeds")
              .put("paymentId", "282ccd65-1319-4b3b-a21c-dfe58809bedf")
              .put("paymentReference", 1881)
              .put("paymentProviderStatus", "FAILED")
              .put("vrns", Collections.singletonList("RD84VSX"))
              .put("isChargedback", false)
              .put("isRefunded", false)
              .build(),

          ImmutableMap.<String, Object>builder()
              .put("paymentTimestamp", "2019-11-24 20:39:08")
              .put("totalPaid", 260)
              .put("cazName", "Leeds")
              .put("paymentId", "485dc5d0-14e1-4007-997e-c2d3cf8b6d1e")
              .put("paymentReference", 998)
              .put("paymentProviderStatus", "SUCCESS")
              .put("vrns", Collections.singletonList("QD84VSX"))
              .put("isChargedback", false)
              .put("isRefunded", true)
              .build(),

          ImmutableMap.<String, Object>builder()
              .put("paymentTimestamp", "2019-11-23 20:39:08")
              .put("totalPaid", 352)
              .put("cazName", "Birmingham")
              .put("paymentId", "391017e8-e2d5-467f-b271-f6cf966eb931")
              .put("paymentReference", 87)
              .put("paymentProviderStatus", "SUCCESS")
              .put("vrns", Arrays.asList("ND84VSX", "MD84VSX", "OD84VSX", "PD84VSX"))
              .put("isChargedback", true)
              .put("isRefunded", false)
              .build()
      );
    }
  }

  private static class PaymentInfoByOperatorIdAssertion {

    private Statistics statistics;
    private RequestSpecification requestSpecification = commonRequestSpecification();
    private ValidatableResponse validatableResponse;

    private PaymentInfoByOperatorIdAssertion(EntityManagerFactory entityManagerFactory) {
      clearHibernateStats(entityManagerFactory);
    }

    public static PaymentInfoByOperatorIdAssertion whenRequested(
        EntityManagerFactory entityManagerFactory) {
      return new PaymentInfoByOperatorIdAssertion(entityManagerFactory);
    }

    private RequestSpecification commonRequestSpecification() {
      return RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
    }

    private void clearHibernateStats(EntityManagerFactory entityManagerFactory) {
      statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
      statistics.clear();
    }

    public PaymentInfoByOperatorIdAssertion andResultsWereFetchedByThreeDatabaseQueries() {
      long queryExecutionCount = getQueriesCount();
      assertThat(queryExecutionCount).isEqualTo(3);
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andResultsWereFetchedByTwoDatabaseQueries() {
      long queryExecutionCount = getQueriesCount();
      assertThat(queryExecutionCount).isEqualTo(2);
      return this;
    }

    private long getQueriesCount() {
      return statistics.getQueryExecutionCount();
    }

    public PaymentInfoByOperatorIdAssertion withPageSizeEqualTo(int pageSize) {
      requestSpecification = requestSpecification.param("pageSize", pageSize);
      return this;
    }

    public PaymentInfoByOperatorIdAssertion withPageNumberEqualTo(int pageNumber) {
      requestSpecification = requestSpecification.param("pageNumber", pageNumber);
      return this;
    }

    public PaymentInfoByOperatorIdAssertion withOperatorIdEqualTo(String operatorId) {
      requestSpecification = requestSpecification.pathParam("operatorId", operatorId);
      return this;
    }

    public PaymentInfoByOperatorIdAssertion then() {
      validatableResponse = requestSpecification.get().then();
      return this;
    }

    public PaymentInfoByOperatorIdAssertion headerContainsCorrelationId() {
      validatableResponse = validatableResponse.header(Constants.X_CORRELATION_ID_HEADER,
          ANY_CORRELATION_ID);
      return this;
    }

    public PaymentInfoByOperatorIdAssertion responseHasOkStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.OK.value());
      return this;
    }

    public PaymentInfoByOperatorIdAssertion responseHasBadRequestStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.BAD_REQUEST.value()).log().all();
      return this;
    }

    public PaymentInfoByOperatorIdAssertion isEmpty() {
      validatableResponse.body("pageCount", equalTo(0));
      validatableResponse.body("totalPaymentsCount", equalTo(0));
      validatableResponse.body("payments", is(emptyIterable()));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andPageIsEqualTo(int expectedPage) {
      validatableResponse.body("page", equalTo(expectedPage));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andPageCountIsEqualTo(int expectedPageCount) {
      validatableResponse.body("pageCount", equalTo(expectedPageCount));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andTotalPaymentsCountIsEqualTo(
        int expectedTotalPaymentsCount) {
      validatableResponse.body("totalPaymentsCount", equalTo(expectedTotalPaymentsCount));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andPaymentsSizeIsEqualTo(int expectedPaymentsSize) {
      validatableResponse.body("payments.size()", equalTo(expectedPaymentsSize));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andContainsErrorMessageEqualTo(String message) {
      validatableResponse.body("message", equalTo(message));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion andPerPageIsEqualTo(int expectedPerPageCount) {
      validatableResponse.body("perPage", equalTo(expectedPerPageCount));
      return this;
    }

    public PaymentInfoByOperatorIdAssertion withRandomOperatorId() {
      return withOperatorIdEqualTo(UUID.randomUUID().toString());
    }

    public PaymentInfoByOperatorIdAssertion andPaymentsEqualTo(
        List<Map<String, Object>> expectedPayments) {
      List<Map<String, Object>> actualPayments = extractResponse();

      assertThat(expectedPayments).hasSameSizeAs(actualPayments);

      Iterator<Map<String, Object>> expectedPaymentsIt = expectedPayments.iterator();
      Iterator<Map<String, Object>> actualPaymentsIt = actualPayments.iterator();
      while (expectedPaymentsIt.hasNext()) {
        Map<String, Object> expectedPayment = expectedPaymentsIt.next();
        Map<String, Object> actualPayment = actualPaymentsIt.next();

        assertThat(withoutVrns(actualPayment)).isEqualTo(withoutVrns(expectedPayment));
        assertThatVrnsMatches(expectedPayment, actualPayment);
      }
      return this;
    }

    private List<Map<String, Object>> extractResponse() {
      Map<String, Object> response = validatableResponse.extract()
          .body()
          .as(new TypeRef<Map<String, Object>>() {});
      return (List<Map<String, Object>>) response.get("payments");
    }

    private void assertThatVrnsMatches(Map<String, Object> expectedPayment,
        Map<String, Object> actualPayment) {
      List<String> actualVrns = (List<String>) actualPayment.get("vrns");
      List<String> expectedVrns = (List<String>) expectedPayment.get("vrns");

      assertThat(actualVrns).containsExactlyInAnyOrderElementsOf(expectedVrns);
    }

    private Map<String, Object> withoutVrns(Map<String, Object> input) {
      Map<String, Object> map = new HashMap<>(input);
      map.remove("vrns");
      return map;
    }
  }

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = PaymentsInfoByOperatorController.PATH;
  }

  @BeforeEach
  public void insertTestData() {
    // we cannot use SQL annotations on this class, see:
    // https://github.com/spring-projects/spring-framework/issues/19930
    clearDatabase();
    executeSqlFrom("data/sql/payment-info-by-operator/test-data.sql");
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

  private PaymentInfoByOperatorIdAssertion whenRequested() {
    return PaymentInfoByOperatorIdAssertion.whenRequested(entityManagerFactory);
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

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
