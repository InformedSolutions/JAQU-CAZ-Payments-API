package uk.gov.caz.psr;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.web.server.LocalServerPort;
import io.restassured.RestAssured;
import uk.gov.caz.psr.journeys.RetrieveAccountVehiclesJourneyAssertion;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;

@FullyRunningServerIntegrationTest
public class RetrieveAccountVehiclesAndChargeabilityIT extends ExternalCallsIT {

  private static final String ZONES =
      "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3,5cd7441d-766f-48ff-b8ad-1809586fea37";
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
    mockAccountServiceOffsetCall(ACCOUNT_ID, "CAS300");
    for (String zone : ZONES.split(",")) {
      mockVccsComplianceCall("CAS300", zone, "vehicle-compliance-response.json", 200);      
    }
    givenVehicleChargesRetrieval()
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
  public void shouldReturn200OkAndResponseWhenZonesNotProvided() {
    mockAccountServiceOffsetCall(ACCOUNT_ID, "CAS300");
    mockVccsCleanAirZonesCall();
    for (String zone : ZONES.split(",")) {
      mockVccsComplianceCall("CAS300", zone, "vehicle-compliance-response.json", 200);      
    }
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsExpectedData();
  }
  
  @Test
  public void shouldReturn200OkAndResponseWhenUnknownVehicleType() {
    mockAccountServiceOffsetCall(ACCOUNT_ID, "CAS302");
    mockVccsUnprocessableEntityComplianceCall("CAS302");
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsTypeUnknownOrUnrecognisedData("CAS302");
  }

  @Test
  public void shouldReturn200OkAndEmptyResponseWhenNoVehiclesReturnedFromAccountsApi() {
    mockAccountServiceOffsetCallWithEmptyResponse(ACCOUNT_ID);
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsEmptyData();
  }
  
  @Test
  public void shouldReturn200OkAndResponseWhenUnrecognisedVrn() {
    mockAccountServiceOffsetCall(ACCOUNT_ID, "ABCDEF");
    mockVccsComplianceCallError("ABCDEF", 404);
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .andResponseContainsTypeUnknownOrUnrecognisedData("ABCDEF");    
  }
  
  @ParameterizedTest
  @CsvSource({",", "0,", ",10"})
  public void shouldReturn400BadRequestWhenQueryParametersNotProvided(
      String pageNumber, String pageSize) {
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber(pageNumber)
      .forPageSize(pageSize)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpErrorStatusCode(400);
  }
  
  @Test
  public void shouldReturn404NotFoundWhenAccountIdNotFound() {
    mockAccountServiceOffsetCallWithError(ACCOUNT_ID, 404);
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpErrorStatusCode(404);
  }
 
  @Test
  public void shouldReturn503WhenVccsUnavailable() {
    mockAccountServiceOffsetCall(ACCOUNT_ID, "CAS300");
    mockVccsComplianceCallError("CAS300", 503);
    givenVehicleChargesRetrieval()
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
    mockAccountServiceOffsetCallWithError(ACCOUNT_ID, 503);
    givenVehicleChargesRetrieval()
      .forAccountId(ACCOUNT_ID)
      .forPageNumber("0")
      .forPageSize("10")
      .forZones(ZONES)
      .whenRequestIsMadeToRetrieveAccountVehicles()
      .then()
      .responseIsReturnedWithHttpErrorStatusCode(503);
  }

  private RetrieveAccountVehiclesJourneyAssertion givenVehicleChargesRetrieval() {
    return new RetrieveAccountVehiclesJourneyAssertion();
  }

}
