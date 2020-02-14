package uk.gov.caz.psr;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import io.restassured.RestAssured;
import uk.gov.caz.psr.journeys.RetrieveAccountVehiclesJourneyAssertion;
import uk.gov.caz.psr.util.MockServerTestIT;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;

@FullyRunningServerIntegrationTest
public class RetrieveAccountVehiclesAndChargeabilityIT extends MockServerTestIT {
  
  private static final String ZONES = 
      "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3,5cd7441d-766f-48ff-b8ad-1809586fea37";

  @LocalServerPort
  int randomServerPort;
  
  @BeforeEach
  public void setupRestAssured() {
      RestAssured.port = randomServerPort;
      RestAssured.baseURI = "http://localhost";
      vccMockServer.reset();
      accountMockServer.reset();
  }
  
  @AfterEach
  public void clearUp() {
    vccMockServer.stop();
    accountMockServer.stop();
  }
  
  @Test
  public void shouldReturn200OkAndResponseWhenValidRequest() {
    givenVehicleRetrieval()
        .forAccountId(UUID.randomUUID().toString())
        .forPageNumber("0")
        .forPageSize("10")
        .forZones(ZONES)
        .mockVehicleCheckerService()
        .mockAccountService()
        .whenRequestIsMadeToRetrieveAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedData();
  }

  private RetrieveAccountVehiclesJourneyAssertion givenVehicleRetrieval() {
    return new RetrieveAccountVehiclesJourneyAssertion(vccMockServer, accountMockServer);
  }

}
