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
import io.restassured.common.mapper.TypeRef;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
import uk.gov.caz.psr.controller.VehicleEntrantController;
import uk.gov.caz.psr.dto.EntrantPaymentWithLatestPaymentDetailsDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.util.AuditTableWrapper;

@FullyRunningServerIntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/add-entrant-payments.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/clear-all-caz-entrant-payments.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class SuccessAddingVehicleEntrantIT {

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
    RestAssured.basePath = VehicleEntrantController.BASE_PATH + "/" +
        VehicleEntrantController.CREATE_VEHICLE_ENTRANT_AND_GET_PAYMENT_DETAILS_PATH;
  }

  @Test
  public void createEntrantPaymentJourney() {
    LocalDateTime dayWithoutEntrantPayment = LocalDateTime.of(2020, 1, 1, 11, 0);

    given()
        .vehicleEntrantRequest(vehicleEntrantRequest(dayWithoutEntrantPayment))
        .whenSubmitted()
        .then()
        .responseContainsDataMatchingRequest()
        .responseContainsExpectedTariffCode(null)
        .entrantPaymentMatchIsNotCreatedInDatabase()
        .entrantPaymentIsCreatedInDatabase()
        .masterRecordCreatedInDatabase()
        .detailRecordCreatedInDatabase(dayWithoutEntrantPayment)
        .andHasVehicleEntrantCapturedSetToTrue();
  }

  @Test
  public void fetchNotPaidEntrantPaymentJourney() {
    LocalDateTime notCapturedDay = LocalDateTime.of(2020, 1, 12, 12, 30);

    given()
        .vehicleEntrantRequest(vehicleEntrantRequest(notCapturedDay))
        .whenSubmitted()
        .then()
        .responseContainsDataMatchingRequest()
        .responseContainsExpectedTariffCode(null)
        .entrantPaymentMatchIsNotCreatedInDatabase()
        .entrantPaymentIsFoundInDatabase()
        .andHasVehicleEntrantCapturedSetToTrue();
  }

  @Test
  public void fetchPaidEntrantPaymentJourney() {
    LocalDateTime capturedDay = LocalDateTime.of(2020, 1, 13, 12, 30);

    given()
        .vehicleEntrantRequest(vehicleEntrantRequest(capturedDay))
        .whenSubmitted()
        .then()
        .responseContainsDataMatchingRequest()
        .responseContainsExpectedTariffCode("TARIFF_CODE_PAID")
        .entrantPaymentMatchIsNotCreatedInDatabase()
        .entrantPaymentIsFoundInDatabase()
        .andHasVehicleEntrantCapturedSetToTrue();
  }

  private VehicleEntrantJourneyAssertion given() {
    return new VehicleEntrantJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private VehicleEntrantDto vehicleEntrantRequest(LocalDateTime cazEntry) {
    VehicleEntrantDto vehicleEntrantDto = VehicleEntrantDto.builder()
        .cleanZoneId(UUID.fromString("4dc6ea23-77d3-4bfe-8180-7662c33f88ad"))
        .cazEntryTimestamp(cazEntry)
        .vrn("EST123")
        .build();

    return vehicleEntrantDto;
  }

  @RequiredArgsConstructor
  static class VehicleEntrantJourneyAssertion {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private VehicleEntrantDto vehicleEntrantDto;
    private List<EntrantPaymentWithLatestPaymentDetailsDto> response;

    public VehicleEntrantJourneyAssertion vehicleEntrantRequest(VehicleEntrantDto request) {
      this.vehicleEntrantDto = request;
      return this;
    }

    public VehicleEntrantJourneyAssertion then() {
      return this;
    }

    public VehicleEntrantJourneyAssertion whenSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      this.response = RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .body(toJsonString(Collections.singletonList(vehicleEntrantDto)))
          .when()
          .post()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
          .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
          .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
          .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
          .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
          .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
          .statusCode(HttpStatus.OK.value())
          .extract().body()
          .as(new TypeRef<List<EntrantPaymentWithLatestPaymentDetailsDto>>() {
          });
      return this;
    }

    public VehicleEntrantJourneyAssertion entrantPaymentIsCreatedInDatabase() {
      verifyThatVehicleEntrantExists();
      return this;
    }

    public VehicleEntrantJourneyAssertion masterRecordCreatedInDatabase() {
      verifyThatMasterTableUpdated();
      return this;
    }

    public VehicleEntrantJourneyAssertion detailRecordCreatedInDatabase(LocalDateTime travelDate) {
      int detailRecordsBefore = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment_audit.t_clean_air_zone_payment_detail",
          "travel_date = '" + travelDate.format(DateTimeFormatter.ISO_DATE) + "'");
      verifyThatDetailTableUpdated(travelDate, detailRecordsBefore);
      return this;
    }

    public VehicleEntrantJourneyAssertion entrantPaymentMatchIsNotCreatedInDatabase() {
      verifyThatNoEntrantPaymentMatchIsCreated();
      return this;
    }

    public VehicleEntrantJourneyAssertion entrantPaymentIsFoundInDatabase() {
      return entrantPaymentIsCreatedInDatabase();
    }

    public VehicleEntrantJourneyAssertion andHasVehicleEntrantCapturedSetToTrue() {
      verifyEntrantPaymentCaptured();
      return this;
    }

    public VehicleEntrantJourneyAssertion responseContainsDataMatchingRequest() {
      EntrantPaymentWithLatestPaymentDetailsDto responseData = response.iterator().next();
      assertThat(responseData.getCleanAirZoneId()).isEqualTo(vehicleEntrantDto.getCleanZoneId());
      assertThat(responseData.getCazEntryTimestamp())
          .isEqualTo(vehicleEntrantDto.getCazEntryTimestamp());
      assertThat(responseData.getVrn()).isEqualTo(vehicleEntrantDto.getVrn());
      return this;
    }

    public VehicleEntrantJourneyAssertion responseContainsExpectedTariffCode(
        String expectedTariffCode) {
      EntrantPaymentWithLatestPaymentDetailsDto responseData = response.iterator().next();
      assertThat(responseData.getTariffCode()).isEqualTo(expectedTariffCode);
      return this;
    }

    private void verifyEntrantPaymentCaptured() {
      int capturedVehiclesCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_payment.t_clean_air_zone_entrant_payment",
              "vehicle_entrant_captured = 'false'");
      assertThat(capturedVehiclesCount).isEqualTo(1);
    }

    private void verifyThatNoEntrantPaymentMatchIsCreated() {
      int entrantPaymentMatchCount = JdbcTestUtils
          .countRowsInTable(jdbcTemplate, "caz_payment.t_clean_air_zone_entrant_payment_match");
      assertThat(entrantPaymentMatchCount).isEqualTo(0);
    }

    private void verifyThatVehicleEntrantExists() {
      VehicleEntrantDto createdRecordData = vehicleEntrantDto;
      int cazEntrantPaymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment",
          "clean_air_zone_id = '" + createdRecordData.getCleanZoneId().toString() + "' AND "
              + "travel_date = '" + createdRecordData.getCazEntryTimestamp().toLocalDate()
              + " ' AND "
              + "vrn = '" + createdRecordData.getVrn() + "'");
      assertThat(cazEntrantPaymentsCount).isEqualTo(1);
    }

    private void verifyThatMasterTableUpdated() {
      VehicleEntrantDto createdRecordData = vehicleEntrantDto;
      int masterRecordCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment_audit.t_clean_air_zone_payment_master",
          "clean_air_zone_id = '" + createdRecordData.getCleanZoneId().toString() + "' AND "
              + "vrn = '" + createdRecordData.getVrn() + "'");
      assertThat(masterRecordCount).isEqualTo(1);
    }

    private void verifyThatDetailTableUpdated(LocalDateTime travelDate, int detailRecords) {
      VehicleEntrantDto createdRecordData = vehicleEntrantDto;
      Object[] params = new Object[]{createdRecordData.getVrn(),
          createdRecordData.getCleanZoneId()};
      UUID masterId = jdbcTemplate.queryForObject(AuditTableWrapper.MASTER_ID_SQL,
          params, UUID.class);
      int detailRecordCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment_audit.t_clean_air_zone_payment_detail",
          "clean_air_zone_payment_master_id = '" + masterId +
              "' AND travel_date = '" + travelDate.format(DateTimeFormatter.ISO_DATE) + "'");
      assertThat(detailRecordCount).isEqualTo(1);
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }
  }
}