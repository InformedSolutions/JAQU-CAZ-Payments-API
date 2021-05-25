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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.params.provider.NullSource;
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
public class PaymentInfoByDatesTestIT {

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static final String VALID_START_DATE = "2019-11-24";
  private static final String VALID_END_DATE = "2019-11-26";
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
    class WhenRequestedWithNullStartDate {

      @ParameterizedTest
      @NullSource
      public void shouldReturnResponseWith400StatusCode(String startDate) {
        whenRequested()
            .withStartDateSizeEqualTo(startDate)
            .withEndDateSizeEqualTo(VALID_END_DATE)
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(1)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'startDate' cannot be null.");
      }
    }

    @Nested
    class WhenRequestedWithNullEndDate {

      @ParameterizedTest
      @NullSource
      public void shouldReturnResponseWith400StatusCode(String endDate) {
        whenRequested()
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(endDate)
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(1)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'endDate' cannot be null.");
      }
    }

    @Nested
    class WhenRequestedWithStartDateAfterEndDateWith400StatusCode {

      @ParameterizedTest
      @ValueSource(strings = "2019-11-23")
      public void shouldReturnResponseWith400StatusCode(String endDate) {
        whenRequested()
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(endDate)
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(1)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .andContainsErrorMessageEqualTo("'startDate' need to be before 'endDate'");
      }
    }

    @Nested
    class WhenRequestedWithEndDateEqualsStarDateWith200StatusCode {

      @ParameterizedTest
      @ValueSource(strings = "2019-11-24")
      public void shouldReturnResponseWith400StatusCode(String endDate) {
        whenRequested()
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(endDate)
            .withPageSizeEqualTo(5)
            .withPageNumberEqualTo(1)
            .then()
            .headerContainsCorrelationId()
            .responseHasOkStatus()
            .andPageIsEqualTo(1)
            .andPageCountIsEqualTo(1)
            .andPerPageIsEqualTo(5)
            .andTotalPaymentsCountIsEqualTo(1)
            .andResultsWereFetchedByTwoDatabaseQueries();
      }
    }

    @Nested
    class WhenRequestedWithNegativePageNumber {

      @ParameterizedTest
      @ValueSource(ints = {-1, -2, -10, -15})
      public void shouldReturnResponseWith400StatusCode(int pageNumber) {
        whenRequested()
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(VALID_END_DATE)
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
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(VALID_END_DATE)
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
  class WhenRequestedWithValidParams {

    @Nested
    class WithDefaultPageSizeAndNumber {

      @Test
      public void shouldReturnPaginatedResultsOnOnePage() {
        mockSuccessVccsCleanAirZonesResponse();
        List<Map<String, Object>> expectedPayments = getExpectedPayments();

        whenRequested()
            .withStartDateSizeEqualTo(VALID_START_DATE)
            .withEndDateSizeEqualTo(VALID_END_DATE)
            .then()
            .headerContainsCorrelationId()
            .responseHasOkStatus()
            .andPageIsEqualTo(0)
            .andPageCountIsEqualTo(1)
            .andPerPageIsEqualTo(10)
            .andTotalPaymentsCountIsEqualTo(3)
            .andPaymentsSizeIsEqualTo(3)
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
          .withStartDateSizeEqualTo(VALID_START_DATE)
          .withEndDateSizeEqualTo(VALID_END_DATE)
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(0)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(0)
          .andPageCountIsEqualTo(3)
          .andTotalPaymentsCountIsEqualTo(3)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(0)))
          .andResultsWereFetchedByThreeDatabaseQueries();

      // second page
      whenRequested()
          .withStartDateSizeEqualTo(VALID_START_DATE)
          .withEndDateSizeEqualTo(VALID_END_DATE)
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(1)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(1)
          .andPageCountIsEqualTo(3)
          .andTotalPaymentsCountIsEqualTo(3)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(1)));

      // third page
      whenRequested()
          .withStartDateSizeEqualTo(VALID_START_DATE)
          .withEndDateSizeEqualTo(VALID_END_DATE)
          .withPageSizeEqualTo(1)
          .withPageNumberEqualTo(2)
          .then()
          .headerContainsCorrelationId()
          .responseHasOkStatus()
          .andPageIsEqualTo(2)
          .andPageCountIsEqualTo(3)
          .andTotalPaymentsCountIsEqualTo(3)
          .andPerPageIsEqualTo(1)
          .andPaymentsSizeIsEqualTo(1)
          .andPaymentsEqualTo(Collections.singletonList(expectedPayments.get(2)));
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
              .put("cazName", "Bath")
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
              .put("cazName", "Bath")
              .put("paymentId", "485dc5d0-14e1-4007-997e-c2d3cf8b6d1e")
              .put("paymentReference", 998)
              .put("paymentProviderStatus", "SUCCESS")
              .put("vrns", Collections.singletonList("QD84VSX"))
              .put("isChargedback", false)
              .put("isRefunded", true)
              .build()
      );
    }
  }

  private static class PaymentInfoByDatesAssertion {

    private Statistics statistics;
    private RequestSpecification requestSpecification = commonRequestSpecification();
    private ValidatableResponse validatableResponse;

    private PaymentInfoByDatesAssertion(EntityManagerFactory entityManagerFactory) {
      clearHibernateStats(entityManagerFactory);
    }

    public static PaymentInfoByDatesAssertion whenRequested(
        EntityManagerFactory entityManagerFactory) {
      return new PaymentInfoByDatesAssertion(entityManagerFactory);
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

    public PaymentInfoByDatesAssertion andResultsWereFetchedByThreeDatabaseQueries() {
      long queryExecutionCount = getQueriesCount();
      assertThat(queryExecutionCount).isEqualTo(3);
      return this;
    }

    public PaymentInfoByDatesAssertion andResultsWereFetchedByTwoDatabaseQueries() {
      long queryExecutionCount = getQueriesCount();
      assertThat(queryExecutionCount).isEqualTo(2);
      return this;
    }

    private long getQueriesCount() {
      return statistics.getQueryExecutionCount();
    }

    public PaymentInfoByDatesAssertion withStartDateSizeEqualTo(String startDate) {
      requestSpecification = requestSpecification.param("startDate", startDate);
      return this;
    }

    public PaymentInfoByDatesAssertion withEndDateSizeEqualTo(String endDate) {
      requestSpecification = requestSpecification.param("endDate", endDate);
      return this;
    }

    public PaymentInfoByDatesAssertion withPageSizeEqualTo(int pageSize) {
      requestSpecification = requestSpecification.param("pageSize", pageSize);
      return this;
    }

    public PaymentInfoByDatesAssertion withPageNumberEqualTo(int pageNumber) {
      requestSpecification = requestSpecification.param("pageNumber", pageNumber);
      return this;
    }

    public PaymentInfoByDatesAssertion then() {
      validatableResponse = requestSpecification.get().then();
      return this;
    }

    public PaymentInfoByDatesAssertion headerContainsCorrelationId() {
      validatableResponse = validatableResponse.header(Constants.X_CORRELATION_ID_HEADER,
          ANY_CORRELATION_ID);
      return this;
    }

    public PaymentInfoByDatesAssertion responseHasOkStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.OK.value());
      return this;
    }

    public PaymentInfoByDatesAssertion responseHasBadRequestStatus() {
      validatableResponse = validatableResponse.statusCode(HttpStatus.BAD_REQUEST.value()).log().all();
      return this;
    }

    public PaymentInfoByDatesAssertion isEmpty() {
      validatableResponse.body("pageCount", equalTo(0));
      validatableResponse.body("totalPaymentsCount", equalTo(0));
      validatableResponse.body("payments", is(emptyIterable()));
      return this;
    }

    public PaymentInfoByDatesAssertion andPageIsEqualTo(int expectedPage) {
      validatableResponse.body("page", equalTo(expectedPage));
      return this;
    }

    public PaymentInfoByDatesAssertion andPageCountIsEqualTo(int expectedPageCount) {
      validatableResponse.body("pageCount", equalTo(expectedPageCount));
      return this;
    }

    public PaymentInfoByDatesAssertion andTotalPaymentsCountIsEqualTo(
        int expectedTotalPaymentsCount) {
      validatableResponse.body("totalPaymentsCount", equalTo(expectedTotalPaymentsCount));
      return this;
    }

    public PaymentInfoByDatesAssertion andPaymentsSizeIsEqualTo(int expectedPaymentsSize) {
      validatableResponse.body("payments.size()", equalTo(expectedPaymentsSize));
      return this;
    }

    public PaymentInfoByDatesAssertion andContainsErrorMessageEqualTo(String message) {
      validatableResponse.body("message", equalTo(message));
      return this;
    }

    public PaymentInfoByDatesAssertion andPerPageIsEqualTo(int expectedPerPageCount) {
      validatableResponse.body("perPage", equalTo(expectedPerPageCount));
      return this;
    }

    public PaymentInfoByDatesAssertion andPaymentsEqualTo(
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
    RestAssured.basePath = PaymentsInfoByOperatorController.OPERATORS_HISTORY_PATH;
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

  private PaymentInfoByDatesAssertion whenRequested() {
    return PaymentInfoByDatesAssertion.whenRequested(entityManagerFactory);
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
