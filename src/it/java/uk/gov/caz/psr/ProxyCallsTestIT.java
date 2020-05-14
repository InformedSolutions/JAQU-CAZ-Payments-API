package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.ProxyController;

@FullyRunningServerIntegrationTest
class ProxyCallsTestIT extends ExternalCallsIT {

  private static ClientAndServer mockWhitelistServer;

  @LocalServerPort
  int randomServerPort;

  @BeforeAll
  public static void startWhitelistMockServer() {
    mockWhitelistServer = startClientAndServer(1092);
  }

  @AfterAll
  public static void stopWhitelistMockServer() {
    mockWhitelistServer.stop();
  }

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/payments";
  }

  @AfterEach
  public void resetMockServer() {
    mockWhitelistServer.reset();
  }

  @Test
  void canFetchCleanAirZones() {
    mockVccsCleanAirZonesCall();
    String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .contentType(MediaType.APPLICATION_JSON.toString())
        .accept(MediaType.APPLICATION_JSON.toString())

        .when()
        .get(ProxyController.GET_CLEAN_AIR_ZONES)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  public void canFetchVehicleDetails() throws Exception {
    String testVrn = "TESTVRN";
    mockVccsVehicleDetailsCall();
    String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .contentType(MediaType.APPLICATION_JSON.toString())
        .accept(MediaType.APPLICATION_JSON.toString())

        .when()
        .get(ProxyController.GET_VEHICLE_DETAILS, testVrn)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  void canFetchVehicleCompliance() throws Exception {
    String testVrn = "TESTVRN";
    String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
    String zone = UUID.randomUUID().toString();
    mockVccsComplianceCall(testVrn, zone, "vehicle-compliance-response.json", 200);

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .contentType(MediaType.APPLICATION_JSON.toString())
        .accept(MediaType.APPLICATION_JSON.toString())
        .param("zones", zone)

        .when()
        .get(ProxyController.GET_COMPLIANCE, testVrn)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  void canFetchUnrecognisedVehicleCompliance() throws Exception {
    String type = "PRIVATE_CAR";
    String zone = UUID.randomUUID().toString();
    String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
    mockVccsUnknownVehicleComplianceCall(type, zone, "unknown-vehicle-compliance-response.json",
        200);

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .contentType(MediaType.APPLICATION_JSON.toString())
        .accept(MediaType.APPLICATION_JSON.toString())
        .param("zones", zone)

        .when()
        .get(ProxyController.GET_UNRECOGNISED_VEHICLE_COMPLIANCE, type)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
  }

  @Nested
  class Whitelisting {

    @Test
    public void shouldSuccessfullyGetWhitelistedVehicleDetails() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      String vrn = "ABC123EF";

      mockSuccessfulResponseFromWhitelistingFor(vrn);

      RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .when()
          .get(ProxyController.GET_WHITELIST_VEHICLE_DETAILS, vrn)
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void shouldReturnNotFoundStatusCodeWhenWhitelistedVehicleIsNotFound() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      String vrn = "ABC123EF";

      mockNotFoundResponseFromWhitelistingFor(vrn);

      RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .when()
          .get(ProxyController.GET_WHITELIST_VEHICLE_DETAILS, vrn)
          .then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private void mockSuccessfulResponseFromWhitelistingFor(String vrn) {
      mockWhitelistServer
          .when(HttpRequest.request()
              .withPath("/v1/whitelisting/vehicles/" + vrn)
              .withMethod("GET"))
          .respond(HttpResponse.response()
              .withStatusCode(HttpStatus.OK.value())
              .withHeader("Content-Type", "application/json")
              .withBody(readFile("whitelist-get-vehicle-details-response.json")));
    }

    private void mockNotFoundResponseFromWhitelistingFor(String vrn) {
      mockWhitelistServer
          .when(HttpRequest.request()
              .withPath("/v1/whitelisting/vehicles/" + vrn)
              .withMethod("GET"))
          .respond(HttpResponse.response()
              .withStatusCode(HttpStatus.NOT_FOUND.value()));
    }
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/json/response/" + filename),
        Charsets.UTF_8);
  }
}
