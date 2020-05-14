package uk.gov.caz.psr;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import com.fasterxml.jackson.core.JsonProcessingException;
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
  public void shouldReturn200OkAndResponseWhenValidRequestAndNoDuplicatePayments() 
      throws JsonProcessingException {
    mockAccountServiceChargesSingleVrnCall(ACCOUNT_ID, "CAS300", 200);
    mockVccsBulkComplianceCall(Collections.singletonList("CAS300"), ZONE, "vehicle-compliance-response-single-zone.json", 200);

    givenSingleAccountVehicleChargeRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forCleanAirZoneId(ZONE)
      .forVrn("CAS300")
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData("CAS300", 0);
  }
  
  @Test
  public void shouldReturn400BadRequestAndResponseWhenZonesNotProvided() {
    givenSingleAccountVehicleChargeRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forVrn("CAS300")
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttp400StatusCode();
  }
  
  @Test
  public void shouldReturn404NotFoundWhenVehicleCannotBeFoundOnAccount() 
      throws JsonProcessingException {
    mockAccountServiceChargesSingleVrnCallWithError(ACCOUNT_ID, "CAS300", 404);
    mockVccsCleanAirZonesCall();
    mockVccsBulkComplianceCall(Collections.singletonList("CAS300"), ZONE, "vehicle-compliance-response-single-zone.json", 200);
    givenSingleAccountVehicleChargeRetrieval()
      .forCleanAirZoneId(ZONE)
      .forVrn("CAS300")
      .forAccountId(ACCOUNT_ID)
      .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
      .then()
      .responseIsReturnedWithHttp404StatusCode();
  }

  private RetrieveSingleChargeableAccountVehicleJourneyAssertion givenSingleAccountVehicleChargeRetrieval() {
    return new RetrieveSingleChargeableAccountVehicleJourneyAssertion();
  }

}
