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
    String accountId = UUID.randomUUID().toString();
    mockVccsComplianceCall("CAS300");
    mockAccountService(accountId);
    givenVehicleRetrieval()
      .forAccountId(accountId)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsExpectedData();
  }
  
  public void mockAccountService(String accountId) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
            exactly(1))
        .respond(response("account-vehicles-response.json"));
  }

  private RetrieveAccountVehiclesJourneyAssertion givenVehicleRetrieval() {
    return new RetrieveAccountVehiclesJourneyAssertion();
  }

}
