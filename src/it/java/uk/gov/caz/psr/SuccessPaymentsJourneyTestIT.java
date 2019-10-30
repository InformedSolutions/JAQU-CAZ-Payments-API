package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.GetAndUpdatePaymentStatusResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class SuccessPaymentsJourneyTestIT {

  @Value("${services.gov-uk-pay.root-url}")
  private String rootUrl;
  @Value("${services.gov-uk-pay.api-key}")
  private String apiKey;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  private ClientAndServer mockServer;

  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/payments";
  }

  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }

  @Test
  public void successPaymentsJourney() {
    String externalPaymentId = "kac1ksqi26f9t2h7q3henmlamc";

    externalPaymentServiceCreatesPaymentWithId(externalPaymentId);
    andReturnsSuccessStatus();

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .paymentEntityIsCreatedInDatabase()
        .withExternalIdEqualTo(externalPaymentId)
        .andResponseIsReturnedWithMatchingInternalId()

        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(PaymentStatus.SUCCESS)
        .andStatusResponseIsReturnedWithMatchinInternalId();
  }

  private PaymentJourneyAssertion given() {
    return new PaymentJourneyAssertion(objectMapper, jdbcTemplate);
  }

  @RequiredArgsConstructor
  static class PaymentJourneyAssertion {
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private InitiatePaymentRequest initiatePaymentRequest;
    private InitiatePaymentResponse initPaymentResponse;
    private GetAndUpdatePaymentStatusResponse getAndUpdatePaymentResponse;

    public PaymentJourneyAssertion initiatePaymentRequest(InitiatePaymentRequest request) {
      this.initiatePaymentRequest = request;
      return this;
    }

    public PaymentJourneyAssertion then() {
      return this;
    }

    public PaymentJourneyAssertion whenSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      this.initPaymentResponse = RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .body(toJsonString(initiatePaymentRequest))
          .when()
          .post()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.CREATED.value())
          .extract()
          .response()
          .as(InitiatePaymentResponse.class);
      return this;
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }

    public PaymentJourneyAssertion paymentEntityIsCreatedInDatabase() {
      verifyThatPaymentEntityExistsWithStatus(PaymentStatus.CREATED);
      return this;
    }

    public PaymentJourneyAssertion withExternalIdEqualTo(String externalPaymentId) {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "external_payment_id = '" + externalPaymentId + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion andResponseIsReturnedWithMatchingInternalId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion and() {
      return this;
    }

    public PaymentJourneyAssertion whenRequestedToGetAndUpdateStatus() {
      String correlationId = "e879d028-2882-4f0b-b3b3-06d7fbcd8537";
      this.getAndUpdatePaymentResponse = RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .when()
          .get(initPaymentResponse.getPaymentId().toString())
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.OK.value())
          .extract().as(GetAndUpdatePaymentStatusResponse.class);
      return this;
    }

    public PaymentJourneyAssertion paymentEntityStatusIsUpdatedTo(PaymentStatus status) {
      verifyThatPaymentEntityExistsWithStatus(status);
      return this;
    }

    private void verifyThatPaymentEntityExistsWithStatus(PaymentStatus status) {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_method = '" + PaymentMethod.CREDIT_CARD.name() + "' AND " +
              "charge_paid = " + initiatePaymentRequest.getAmount() + " AND " +
              "status = '" + status.name() + "' AND " +
              "caz_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "'");
      assertThat(paymentsCount).isEqualTo(1);
    }

    public PaymentJourneyAssertion andStatusResponseIsReturnedWithMatchinInternalId() {
      assertThat(getAndUpdatePaymentResponse.getPaymentId())
          .isEqualTo(initPaymentResponse.getPaymentId());
      return this;
    }
  }

  private InitiatePaymentRequest initiatePaymentRequest() {
    return InitiatePaymentRequest.builder()
        .amount(4200)
        .days(Collections.singletonList(LocalDate.now()))
        .cleanAirZoneId(UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4"))
        .returnUrl("http://localhost/return-url")
        .vrn("ND84VSX")
        .build();
  }

  private void externalPaymentServiceCreatesPaymentWithId(String externalPaymentId) {
    // externalPaymentId - not used, its value is set in data/external/create-payment-response.json
    mockServer.when(
        HttpRequest.request()
            .withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withPath(ExternalPaymentsRepository.CREATE_URI)
    ).respond(
        HttpResponse.response()
            .withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("create-payment-response.json"))
    );
  }

  private void andReturnsSuccessStatus() {
    mockServer.when(
        HttpRequest.request()
            .withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/payments/.*")
    ).respond(
        HttpResponse.response()
            .withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-payment-response.json"))
    );
  }

  /// ----- utility methods

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
