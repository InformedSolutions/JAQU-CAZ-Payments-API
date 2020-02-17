package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.boot.web.server.LocalServerPort;
import io.restassured.RestAssured;
import uk.gov.caz.psr.journeys.RetrieveAccountVehiclesJourneyAssertion;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;

@FullyRunningServerIntegrationTest
public class RetrieveAccountVehiclesAndChargeabilityIT extends VccsCallsIT {

  private static final String ZONES =
      "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3,5cd7441d-766f-48ff-b8ad-1809586fea37";
  private static final String ACCOUNT_ID = UUID.randomUUID().toString();

  @LocalServerPort
  int randomServerPort;
  
  private ClientAndServer accountsMockServer;

  @BeforeEach
  public void setupRestAssured() {
    accountsMockServer = startClientAndServer(1091);
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }
  
  @AfterEach
  public void stopMockServer() {
    accountsMockServer.stop();
  }

  @Test
  public void shouldReturn200OkAndResponseWhenValidRequest() {
    mockAccountService(ACCOUNT_ID, "CAS300");
    mockVccsComplianceCall("CAS300", "vehicle-compliance-response.json", 200);
    givenVehicleRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsExpectedData();
  }
  
  @Test
  public void shouldReturn200OkAndResponseWhenUnknownVehicleType() {
    mockAccountService(ACCOUNT_ID, "CAS302");
    mockVccsComplianceCall("CAS302", "vehicle-compliance-null-response.json", 422);
    givenVehicleRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsTypeUnknownData();
  }
  
  @Test
  public void shouldReturn503WhenVccsUnavailable() {
    mockAccountService(ACCOUNT_ID, "CAS300");
    mockVccsComplianceCallError("CAS300", 503);
    givenVehicleRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpErrorStatusCode(503);
  }
  
  @Test
  public void shouldReturn503WhenAccountServiceUnavailable() {
    mockAccountServiceError(ACCOUNT_ID, 503);
    givenVehicleRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpErrorStatusCode(503);
  }
  
  private void mockAccountService(String accountId, String vrn) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
            exactly(1))
        .respond(response("account-vehicles-response.json", vrn, 200));
  }
  
  private void mockAccountServiceError(String accountId, int statusCode) {
    accountsMockServer
      .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
          exactly(1))
      .respond(emptyResponse(503));
  }

  private RetrieveAccountVehiclesJourneyAssertion givenVehicleRetrieval() {
    return new RetrieveAccountVehiclesJourneyAssertion();
  }

}
