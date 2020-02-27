package uk.gov.caz.psr;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import io.restassured.RestAssured;
import uk.gov.caz.psr.journeys.RetrieveSingleChargeableAccountVehicleJourneyAssertion;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;

@FullyRunningServerIntegrationTest
public class RetrieveSingleAccountVehicleAndChargeabilityIT extends ExternalCallsIT {

  private static final String ZONE =
      "5cd7441d-766f-48ff-b8ad-1809586fea37";
  private static final String ACCOUNT_ID = UUID.randomUUID().toString();

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }
  
  @Test
  public void shouldReturn200OkAndResponseWhenValidRequest() {
    mockAccountServiceChargesCall(ACCOUNT_ID, "CAS300");
    mockVccsComplianceCall("CAS300", "vehicle-compliance-response.json", 200);

    givenSingleAccountVehicleChargeRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forCleanAirZoneId(ZONE)
      .forVrn("CAS300")
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttpOkStatusCode();
  }
  
  @Test
  public void shouldReturn400BadRequestAndResponseWhenZonesNotProvided() {
    mockAccountServiceChargesCall(ACCOUNT_ID, "CAS300");
    mockVccsCleanAirZonesCall();
    mockVccsComplianceCall("CAS300", "vehicle-compliance-response.json", 200);
    givenSingleAccountVehicleChargeRetrieval()
      .forAccountId(ACCOUNT_ID)
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttp400StatusCode();
  }
  
  @Test
  public void shouldReturn404NotFoundWhenVehicleCannotBeFoundOnAccount() {
    mockAccountServiceChargesCall(ACCOUNT_ID, "CAS300");
    mockVccsCleanAirZonesCall();
    mockVccsComplianceCall("CAS300", "vehicle-compliance-response.json", 200);
    givenSingleAccountVehicleChargeRetrieval()
      .forAccountId(UUID.randomUUID().toString())
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttp400StatusCode();
  }

  private RetrieveSingleChargeableAccountVehicleJourneyAssertion givenSingleAccountVehicleChargeRetrieval() {
    return new RetrieveSingleChargeableAccountVehicleJourneyAssertion();
  }

}
