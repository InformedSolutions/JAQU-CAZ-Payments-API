package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.caz.psr.controller.Headers;
import uk.gov.caz.psr.dto.PaymentStatusUpdateDetails;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusUpdateDetailsFactory;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/add-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
public class SuccessPaymentStatusUpdateTestIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void startMockServer() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = ChargeSettlementController.BASE_PATH +
        ChargeSettlementController.PAYMENT_STATUS_PATH;
  }

  @Test
  public void successPaymentStatusUpdateJourney() {
    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequest())
        .whenSubmitted()
        .then()
        .vehicleEntrantPaymentsAreUpdatedInTheDatabse();
  }

  private PaymentStatusUpdateJourneyAssertion given() {
    return new PaymentStatusUpdateJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequest() {
    return PaymentStatusUpdateRequest.builder()
        .vrn("ND84VSX")
        .statusUpdates(validStatusUpdates())
        .build();
  }

  private List<PaymentStatusUpdateDetails> validStatusUpdates() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.refundedWithDateOfCazEntry(LocalDate.of(2019, 11, 1)),
        PaymentStatusUpdateDetailsFactory
            .refundedWithDateOfCazEntryAndPaymentId(LocalDate.of(2019, 11, 3), "12345test")
    );
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

    public PaymentStatusUpdateJourneyAssertion whenSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
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
          .statusCode(HttpStatus.OK.value());
      return this;
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }

    public PaymentStatusUpdateJourneyAssertion vehicleEntrantPaymentsAreUpdatedInTheDatabse() {
      verifyThatRefundedVehicleEntrantPaymentsExists();
      return this;
    }

    private void verifyThatRefundedVehicleEntrantPaymentsExists() {
      int vehicleEntrantCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment",
          "caz_id = '" + cleanAirZoneId.toString() + "' AND "
              + "vrn = '" + paymentStatusUpdateRequest.getVrn() + "' AND "
              + "payment_status = '" + InternalPaymentStatus.REFUNDED.name() + "'");
      assertThat(vehicleEntrantCount).isEqualTo(2);
    }
  }
}