package uk.gov.caz.psr.journeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.controller.AccountsController;

@RequiredArgsConstructor
public class RetrieveAccountVehiclesJourneyAssertion {
  private static final String CORRELATION_ID = UUID.randomUUID().toString();
  
  private String accountId;
  private String pageNumber;
  private String pageSize;
  private String zones;
  
  private ValidatableResponse vehicleResponse;
  private VehicleRetrievalResponseDto vehicleResponseDto;

  public RetrieveAccountVehiclesJourneyAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion forPageNumber(String pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion forPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion forZones(String zones) {
    this.zones = zones;
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion whenRequestIsMadeToRetrieveAccountVehicles() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
    this.vehicleResponse = RestAssured
      .given()
      .accept(MediaType.APPLICATION_JSON.toString())
      .contentType(MediaType.APPLICATION_JSON.toString())
      .pathParam("account_id", this.accountId)
      .queryParam("pageNumber", pageNumber)
      .queryParam("pageSize", pageSize)
      .queryParam("zones", zones)
      .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
      .when()
      .get("/{account_id}/charges")
      .then();
    return this;
  }
  
  public RetrieveAccountVehiclesJourneyAssertion then() {
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    this.vehicleResponseDto = vehicleResponse.statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID).extract()
        .as(VehicleRetrievalResponseDto.class);
    return this;
  }

  public RetrieveAccountVehiclesJourneyAssertion responseIsReturnedWithHttpErrorStatusCode(
      int statusCode) {
    vehicleResponse.statusCode(statusCode);
    return this;
  }

  public void andResponseContainsExpectedData() {
    checkResponseSize();
    assertEquals("CAS300", this.vehicleResponseDto.getVehicles().get(0).getRegistrationNumber());
  }

  public void andResponseContainsTypeUnknownData() {
    checkResponseSize();
    ComplianceResultsDto firstVehicle = this.vehicleResponseDto.getVehicles().get(0);
    assertEquals("CAS302", firstVehicle.getRegistrationNumber());
    assertEquals(null, firstVehicle.getIsExempt());
    assertEquals(null, firstVehicle.getIsRetrofitted());
    assertEquals(null, firstVehicle.getVehicleType());
    assertTrue(firstVehicle.getComplianceOutcomes().isEmpty());
  }

  private void checkResponseSize() {
    assertEquals(1, this.vehicleResponseDto.getVehicles().size());
    assertEquals(Integer.parseInt(this.pageNumber), this.vehicleResponseDto.getPage());
    assertEquals(Integer.parseInt(this.pageSize), this.vehicleResponseDto.getPerPage());
    assertEquals(1,this.vehicleResponseDto.getPageCount());
    assertEquals(1, this.vehicleResponseDto.getTotalVrnsCount());    
  }
}
