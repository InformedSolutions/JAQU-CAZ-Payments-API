package uk.gov.caz.psr;

import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.journeys.RetrieveSingleChargeableAccountVehicleJourneyAssertion;

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
  public void shouldReturn200OkAndResponseWhenValid() {
    mockAccountServiceChargesSingleVrnCall(ACCOUNT_ID, "CAS300", 200);

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
  public void shouldReturn404NotFoundWhenVehicleFoundButIsNotChargeableInCAZ() {
    mockAccountServiceChargesSingleNoChargeVrnCall(ACCOUNT_ID, "CAS300");

    givenSingleAccountVehicleChargeRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forCleanAirZoneId(ZONE)
        .forVrn("CAS300")
        .whenRequestIsMadeToRetrieveASingleChargeableAccountVehicle()
        .then()
        .responseIsReturnedWithHttp404StatusCode();
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
  public void shouldReturn404NotFoundWhenVehicleCannotBeFoundOnAccount() {
    mockAccountServiceChargesSingleVrnCallWithError(ACCOUNT_ID, "CAS300", 404);
    mockVccsCleanAirZonesCall();
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
