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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.caz.psr.util.AuditTableWrapper;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusUpdateDetailsFactory;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/add-payments-for-payment-status.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class SuccessPaymentStatusUpdateTestIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final String FORMATTED_VRN = "ND84VSX";
  private static final LocalDate TRAVEL_DATE_ONE = LocalDate.of(2019, 11, 1);
  private static final LocalDate TRAVEL_DATE_TWO = LocalDate.of(2019, 11, 3);

  @BeforeEach
  public void startMockServer() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = ChargeSettlementController.BASE_PATH +
        ChargeSettlementController.PAYMENT_STATUS_PATH;
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ND84VSX",
      "Nd84vSX", "nD84vsX",
      "ND 84 VSX", "  ND84V S X ", "N D8   4VSX",
      "N D8  4v SX "
  })
  public void successPaymentStatusUpdateJourney(String vrn) {
    given()
        .paymentStatusUpdateRequest(paymentStatusUpdateRequest(vrn))
        .whenSubmitted()
        .then()
        .entrantPaymentsAreUpdatedInTheDatabase()
        .masterRecordExistsForVrn(FORMATTED_VRN)
        .detailTableIsUpdated(FORMATTED_VRN);
  }

  private PaymentStatusUpdateJourneyAssertion given() {
    return new PaymentStatusUpdateJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private PaymentStatusUpdateRequest paymentStatusUpdateRequest(String vrn) {
    return PaymentStatusUpdateRequest.builder()
        .vrn(vrn)
        .statusUpdates(validStatusUpdates())
        .build();
  }

  private List<PaymentStatusUpdateDetails> validStatusUpdates() {
    return Arrays.asList(
        PaymentStatusUpdateDetailsFactory.refundedWithDateOfCazEntry(TRAVEL_DATE_ONE),
        PaymentStatusUpdateDetailsFactory
            .refundedWithDateOfCazEntry(TRAVEL_DATE_TWO)
    );
  }

  @RequiredArgsConstructor
  static class PaymentStatusUpdateJourneyAssertion {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final UUID cleanAirZoneId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");
    private final int EXPECTED_NUMBER_OF_REFUNDED_RECORDS = 4;

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
          .header(Headers.TIMESTAMP, LocalDateTime.now().toString())
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

    public PaymentStatusUpdateJourneyAssertion entrantPaymentsAreUpdatedInTheDatabase() {
      verifyThatRefundedEntrantPaymentsExists();
      return this;
    }

    private void verifyThatRefundedEntrantPaymentsExists() {
      int vehicleEntrantCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment",
          "clean_air_zone_id = '" + cleanAirZoneId.toString() + "' AND "
              + "vrn = 'ND84VSX' AND "
              + "payment_status = '" + InternalPaymentStatus.REFUNDED.name() + "'");
      assertThat(vehicleEntrantCount).isEqualTo(EXPECTED_NUMBER_OF_REFUNDED_RECORDS);
    }

    public PaymentStatusUpdateJourneyAssertion masterRecordExistsForVrn(String vrn) {
      verifyThatMasterRecordExistsForVrnAndCleanAirZone(vrn);
      return this;
    }
    
    private void verifyThatMasterRecordExistsForVrnAndCleanAirZone(String vrn) {
      int masterCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, AuditTableWrapper.MASTER, 
          "vrn = '" + vrn + "' AND clean_air_zone_id = '" + cleanAirZoneId + "'");
      assertThat(masterCount).isEqualTo(1);
    }

    public void detailTableIsUpdated(String vrn) {
      verifyThatDetailRecordExistsForNewPaymentStatusForBothDates(vrn);      
    }
    
    private void verifyThatDetailRecordExistsForNewPaymentStatusForBothDates(String vrn) {
      Object[] params = new Object[] {vrn, cleanAirZoneId};
      UUID masterId = jdbcTemplate.queryForObject(AuditTableWrapper.MASTER_ID_SQL, params, UUID.class);
      int detailDay1Count = getDetailRecordNumberForVrnAndCleanAirZoneAndTravelDate(masterId, TRAVEL_DATE_ONE);
      int detailDay2Count = getDetailRecordNumberForVrnAndCleanAirZoneAndTravelDate(masterId, TRAVEL_DATE_TWO);
      assertThat(detailDay1Count + detailDay2Count).isEqualTo(2);
    }
    
    private int getDetailRecordNumberForVrnAndCleanAirZoneAndTravelDate(UUID masterId, LocalDate travelDate) {
      return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          AuditTableWrapper.DETAIL,"payment_status = '" + InternalPaymentStatus.REFUNDED.name() 
          + "' AND " + AuditTableWrapper.MASTER_ID + " = '" + masterId
          + "' AND travel_date = '" + travelDate.format(DateTimeFormatter.ISO_DATE) + "'");
    }
  }
}
