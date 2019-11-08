package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
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
import uk.gov.caz.psr.dto.VehicleEntrantRequest;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/add-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/clear-all-vehicle-entrants.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class SuccessAddingVehicleEntrantIT {

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
    RestAssured.basePath = "/v1/payments/vehicle-entrants";
  }

  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }

  @Test
  public void createVehicleEntrantJourney() {
    LocalDateTime paidDay = LocalDateTime.of(2019, 11, 2, 15, 50);
    LocalDateTime notPaidDay = LocalDateTime.of(2019, 11, 10, 15, 50);

    given()
        .vehicleEntrantRequest(vehicleEntrantRequest(notPaidDay))
        .whenSubmitted()
        .then()
        .vehicleEntrantIsCreatedInDatabase()
        .vehicleEntrantIsNotConnectedToVehicleEntrantPayments();

    given()
        .vehicleEntrantRequest(vehicleEntrantRequest(paidDay))
        .whenSubmitted()
        .then()
        .vehicleEntrantIsCreatedInDatabase()
        .vehicleEntrantIsConnectedToVehicleEntrantPayments();
  }

  private VehicleEntrantJourneyAssertion given() {
    return new VehicleEntrantJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private VehicleEntrantRequest vehicleEntrantRequest(LocalDateTime cazEntry) {
    return VehicleEntrantRequest.builder()
        .cleanZoneId(UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4"))
        .cazEntryTimestamp(cazEntry)
        .vrn("ND84VSX")
        .build();
  }

  @RequiredArgsConstructor
  static class VehicleEntrantJourneyAssertion {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private VehicleEntrantRequest vehicleEntrantRequest;

    public VehicleEntrantJourneyAssertion vehicleEntrantRequest(VehicleEntrantRequest request) {
      this.vehicleEntrantRequest = request;
      return this;
    }

    public VehicleEntrantJourneyAssertion then() {
      return this;
    }

    public VehicleEntrantJourneyAssertion whenSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      RestAssured
          .given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .body(toJsonString(vehicleEntrantRequest))
          .when()
          .post()
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.OK.value())
          .extract()
          .response();
      return this;
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }

    public VehicleEntrantJourneyAssertion vehicleEntrantIsCreatedInDatabase() {
      verifyThatVehicleEntrantExists();
      return this;
    }

    private void verifyThatVehicleEntrantExists() {
      int vehicleEntrantCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant",
          "caz_id = '" + vehicleEntrantRequest.getCleanZoneId().toString() + "' AND "
              + "caz_entry_timestamp = '" + vehicleEntrantRequest.getCazEntryTimestamp() + " ' AND "
              + "vrn = '" + vehicleEntrantRequest.getVrn() + "'");
      assertThat(vehicleEntrantCount).isEqualTo(1);
    }

    public VehicleEntrantJourneyAssertion vehicleEntrantIsNotConnectedToVehicleEntrantPayments() {
      verifyThatNoVehicleEntrantPaymentsIsAssigned();
      return this;
    }

    private void verifyThatNoVehicleEntrantPaymentsIsAssigned() {
      int vehicleEntrantPaymentCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment",
          "caz_id = '" + vehicleEntrantRequest.getCleanZoneId().toString() + "' AND "
              + "travel_date = '" + vehicleEntrantRequest.getCazEntryTimestamp().toLocalDate() + " ' AND "
              + "vrn = '" + vehicleEntrantRequest.getVrn() + "' AND "
              + "payment_status = 'PAID' AND "
              + "vehicle_entrant_id is not null");
      assertThat(vehicleEntrantPaymentCount).isEqualTo(0);
    }

    public VehicleEntrantJourneyAssertion vehicleEntrantIsConnectedToVehicleEntrantPayments() {
      verifyThatVehicleEntrantPaymentsIsAssigned();
      return this;
    }

    private void verifyThatVehicleEntrantPaymentsIsAssigned() {
      int vehicleEntrantPaymentCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment",
          "caz_id = '" + vehicleEntrantRequest.getCleanZoneId().toString() + "' AND "
              + "travel_date = '" + vehicleEntrantRequest.getCazEntryTimestamp().toLocalDate() + " ' AND "
              + "vrn = '" + vehicleEntrantRequest.getVrn() + "' AND "
              + "payment_status = 'PAID' AND "
              + "vehicle_entrant_id is not null");
      assertThat(vehicleEntrantPaymentCount).isEqualTo(1);
    }
  }
}