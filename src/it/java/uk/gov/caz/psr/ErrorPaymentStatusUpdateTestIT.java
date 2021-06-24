package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.ChargeSettlementController;
import uk.gov.caz.psr.dto.Headers;
import uk.gov.caz.psr.dto.PaymentStatusUpdateDetails;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusUpdateDetailsFactory;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/add-payments-for-payment-status.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class ErrorPaymentStatusUpdateTestIT {

  private static final String VALID_VRN = "ND84VSX";

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final String TEST_VRN = "CAS300";
  private static final String NOT_NULL = "must not be null";
  private static final String NOT_EMPTY = " is mandatory and cannot be blank";
  private static final String INCORRECT_STATUS = "Incorrect payment status update, please use "
      + "\"paid\", \"chargeback\", \"refunded\" or \"failed\" instead";
  private static final PaymentStatusUpdateDetails INVALID_TRAVEL_DATE =
      PaymentStatusUpdateDetailsFactory.refundedWithDateOfCazEntry(LocalDate.of(2019, 11, 1));
  private static final String NON_EXISTING_VRN = "MARYSIA";
  private static final String REQUEST_WITHOUT_PAYMENT_STATUS =
      "{\"vrn\":\"" + TEST_VRN + "\",\"statusUpdates\""
          + ":[{\"dateOfCazEntry\":\"2020-02-05\",\"paymentProviderId\":\"akor5rnblpfdasco6pp04dc5if\","
          + "\"caseReference\":\"test123\"}]}";

  @BeforeEach
  public void startMockServer() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = ChargeSettlementController.PAYMENT_STATUS_PATH;
  }

  @Test
  public void paymentStatusUpdateWithoutVrn() {
    // when invalid request submitted
    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequestWithoutVrn())
        .whenRequestSubmitted("vrn", NOT_EMPTY)
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  @Test
  public void errorsPaymentStatusUpdateWithUnfinishedPayment() {
    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequestWithUnfinishedPayment())
        .whenRequestSubmitted()
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  @Test
  public void errorsPaymentStatusUpdateForNonExistingEntrantPaymentJourney() {
    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequestForNonExistingParams())
        .whenNonExistentRequestSubmitted(NON_EXISTING_VRN)
        .then()
        .noEntrantPaymentUpdatedInDatabase();

    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequestWithInvalidPaymentStatus())
        .failsWhenInvalidStatusRequestSubmitted();
  }

  @Test
  public void paymentStatusUpdateWithoutDateOfCazEntry() {
    given().paymentStatusUpdateRequest(paymentStatusUpdateRequestWithoutDateOfCazEntry())
        .whenRequestSubmitted("dateOfCazEntry", NOT_NULL)
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  @Test
  public void paymentStatusUpdateWithoutPaymentStatus() {
    given().paymentStatusUpdateRequest(paymentStatusUpdateRequestWithoutPaymentStatus())
        .whenPaymentStatusRequestSubmitted("paymentStatus", NOT_NULL, INCORRECT_STATUS,
            REQUEST_WITHOUT_PAYMENT_STATUS)
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  @Test
  public void paymentStatusUpdateWithoutCaseReference() {
    given().paymentStatusUpdateRequest(paymentStatusUpdateRequestWithoutCaseReference())
        .whenRequestSubmitted("caseReference", NOT_EMPTY)
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  @Test
  public void paymentStatusUpdateWithInvalidCaseReference() {
    given().paymentStatusUpdateRequest(paymentStatusUpdateRequestWithInvalidCaseReference())
        .whenRequestSubmitted("caseReference", "Update caseReference to a valid format")
        .then()
        .noEntrantPaymentUpdatedInDatabase();
  }

  private PaymentStatusUpdateJourneyAssertion given() {
    return new PaymentStatusUpdateJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithoutVrn() {
    return PaymentStatusUpdateRequest.builder()
        .statusUpdates(exampleStatusUpdates())
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestForNonExistingParams() {
    return PaymentStatusUpdateRequest.builder()
        .vrn(NON_EXISTING_VRN)
        .statusUpdates(Arrays.asList(INVALID_TRAVEL_DATE))
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithoutDateOfCazEntry() {
    return PaymentStatusUpdateRequest.builder()
        .vrn(TEST_VRN)
        .statusUpdates(statusUpdatesWithoutDateOfCazEntry())
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithoutCaseReference() {
    return PaymentStatusUpdateRequest.builder()
        .vrn(TEST_VRN)
        .statusUpdates(statusUpdatesWithoutCaseReference())
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithInvalidCaseReference() {
    return PaymentStatusUpdateRequest.builder()
        .vrn(TEST_VRN)
        .statusUpdates(statusUpdatesWithInvalidCaseReference())
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithoutPaymentStatus() {
    return PaymentStatusUpdateRequest.builder()
        .vrn(TEST_VRN)
        .statusUpdates(statusUpdatesWithoutPaymentStatus())
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithUnfinishedPayment() {
    return PaymentStatusUpdateRequest.builder()
        .statusUpdates(unfinishedPaymentStatusUpdate())
        .vrn("CAS123")
        .build();
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequestWithInvalidPaymentStatus() {
    return PaymentStatusUpdateRequest.builder()
        .statusUpdates(invalidPaymentStatusUpdate())
        .vrn(VALID_VRN)
        .build();
  }

  private List<PaymentStatusUpdateDetails> exampleStatusUpdates() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.refundedWithDateOfCazEntry(LocalDate.of(2019, 11, 2))
    );
  }

  private List<PaymentStatusUpdateDetails> statusUpdatesWithoutCaseReference() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.anyWithoutCaseReference()
    );
  }

  private List<PaymentStatusUpdateDetails> statusUpdatesWithInvalidCaseReference() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.invalidCaseReference()
    );
  }

  private List<PaymentStatusUpdateDetails> statusUpdatesWithoutDateOfCazEntry() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.anyWithoutDateOfCazEntry()
    );
  }

  private List<PaymentStatusUpdateDetails> statusUpdatesWithoutPaymentStatus() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.anyWithoutPaymentStatus()
    );
  }

  private List<PaymentStatusUpdateDetails> unfinishedPaymentStatusUpdate() {
    PaymentStatusUpdateDetails details = PaymentStatusUpdateDetails.builder()
        .caseReference("case-ref-14")
        .dateOfCazEntry(LocalDate.parse("2019-11-05"))
        .paymentStatus("refunded")
        .build();

    return Arrays.asList(details);
  }

  private List<PaymentStatusUpdateDetails> invalidPaymentStatusUpdate() {
    PaymentStatusUpdateDetails details = PaymentStatusUpdateDetails.builder()
        .caseReference("case-ref-14")
        .dateOfCazEntry(LocalDate.parse("2019-11-01"))
        .paymentStatus("Refunded") // Invalid payment status
        .build();

    return Arrays.asList(details);
  }

  @RequiredArgsConstructor
  static class PaymentStatusUpdateJourneyAssertion {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final UUID cleanAirZoneId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    private PaymentStatusUpdateRequest paymentStatusUpdateRequest;

    public PaymentStatusUpdateJourneyAssertion paymentStatusUpdateRequest(
        PaymentStatusUpdateRequest request) {
      this.paymentStatusUpdateRequest = request;
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion then() {
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion whenRequestSubmitted(String errorField, String msg) {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
          .header(Headers.X_API_KEY, cleanAirZoneId)
          .body(toJsonString(paymentStatusUpdateRequest))
          .when()
          .put()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
          .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
          .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
          .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
          .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
          .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
          .statusCode(HttpStatus.BAD_REQUEST.value())
          .body("errors[0].vrn", equalTo(errorField.equals("vrn") ? null : TEST_VRN))
          .body("errors[0].field", equalTo(errorField))
          .body("errors[0].detail", containsString(msg));
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion whenPaymentStatusRequestSubmitted(String errorField,
        String msg,
        String msg2, String body) {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
          .header(Headers.X_API_KEY, cleanAirZoneId)
          .body(body)
          .when()
          .put()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
          .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
          .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
          .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
          .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
          .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
          .statusCode(HttpStatus.BAD_REQUEST.value())
          .body("errors[0].vrn", equalTo(errorField.equals("vrn") ? null : TEST_VRN))
          .body("errors[0].field", equalTo(errorField))
          .body("errors[0].detail", anyOf(containsString(msg), containsString(msg2)))
          .body("errors[1].detail", anyOf(containsString(msg), containsString(msg2)));
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion failsWhenInvalidStatusRequestSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
          .header(Headers.X_API_KEY, cleanAirZoneId)
          .body(toJsonString(paymentStatusUpdateRequest))
          .when()
          .put()
          .then()
          .statusCode(HttpStatus.BAD_REQUEST.value())
          .body("errors[0].vrn", equalTo(VALID_VRN))
          .body("errors[0].detail", containsString(
              "Incorrect payment status update, please use \"paid\", \"chargeback\", \"refunded\" or \"failed\" instead"));
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion whenRequestSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
          .header(Headers.X_API_KEY, cleanAirZoneId)
          .body(toJsonString(paymentStatusUpdateRequest))
          .when()
          .put()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
          .body("status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()))
          .body("message", containsString("Payment is still being processed"));
      return this;
    }

    public PaymentStatusUpdateJourneyAssertion whenNonExistentRequestSubmitted(String vrn) {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
          .header(Headers.X_API_KEY, cleanAirZoneId)
          .body(toJsonString(paymentStatusUpdateRequest))
          .when()
          .put()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.BAD_REQUEST.value())
          .body("errors[0].status", equalTo(400))
          .body("errors[0].vrn", equalTo(vrn))
          .body("errors[0].title", equalTo("Vehicle entry not found"))
          .body("errors[0].detail",
              containsString(
                  "A vehicle entry for the supplied combination of vrn and date of CAZ entry"));
      return this;

    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request).replace("REFUNDED", "refunded");
    }

    public PaymentStatusUpdateJourneyAssertion noEntrantPaymentUpdatedInDatabase() {
      verifyNoVehicleEntrantPaymentUpdated();
      return this;
    }

    private void verifyNoVehicleEntrantPaymentUpdated() {
      int vehicleEntrantPaymentCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment",
          "clean_air_zone_id = '" + cleanAirZoneId.toString() + "' AND "
              + "vrn = '" + paymentStatusUpdateRequest.getVrn() + "' AND "
              + "payment_status = '" + InternalPaymentStatus.REFUNDED.name() + "'");
      assertThat(vehicleEntrantPaymentCount).isEqualTo(0);
    }
  }
}
