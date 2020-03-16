package uk.gov.caz.psr.journeys;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.controller.AccountsController;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;

public class RetrieveChargeableAccountVehiclesJourneyAssertion {
  
  private String accountId;
  private String direction;
  private String pageSize;
  private String vrn;
  private String cleanAirZoneId;
  private ValidatableResponse response;
  private ChargeableAccountVehicleResponse responseDto;
  private static final String CORRELATION_ID = UUID.randomUUID().toString();
  
  public RetrieveChargeableAccountVehiclesJourneyAssertion() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;    
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forDirection(String direction) {
    this.direction = direction;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forVrn(String vrn) {
    this.vrn = vrn;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forCleanAirZoneId(String cleanAirZoneId) {
    this.cleanAirZoneId = cleanAirZoneId;
    return this;
  }
  
  public RetrieveChargeableAccountVehiclesJourneyAssertion 
  whenRequestIsMadeToRetrieveChargeableAccountVehicles() {
    this.response = RestAssured
      .given()
      .accept(MediaType.APPLICATION_JSON.toString())
      .contentType(MediaType.APPLICATION_JSON.toString())
      .pathParam("account_id", this.accountId)
      .queryParam("direction", this.direction)
      .queryParam("pageSize", this.pageSize)
      .queryParam("vrn", this.vrn)
      .queryParam("cleanAirZoneId", this.cleanAirZoneId)
      .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
      .when()
      .get("/{account_id}/chargeable-vehicles")
      .then();
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion 
  whenRequestIsMadeToRetrieveChargeableAccountVehiclesWithoutQueryStrings() {
    this.response = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .pathParam("account_id", this.accountId)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get("/{account_id}/chargeable-vehicles")
        .then();
      return this;
  }
  
  public RetrieveChargeableAccountVehiclesJourneyAssertion then() {
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    this.responseDto = response.statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID).extract()
        .as(ChargeableAccountVehicleResponse.class);
    return this;
  }
  
  public void responseIsReturnedWithStatusCode(int statusCode) {
    response.statusCode(statusCode);
  }

  public void responseContainsExpectedData(List<String> expectedVrns, 
      String firstVrn, String lastVrn) {
    List<VrnWithTariffAndEntrancesPaid> results = this.responseDto.getChargeableAccountVehicles().getResults();
    assertTrue(this.responseDto.getChargeableAccountVehicles().getResults().size() <= Integer.parseInt(this.pageSize));
    assertEquals(expectedVrns, results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    for (VrnWithTariffAndEntrancesPaid result: results) {
      assertTrue(result.getCharge() > 0);
      assertNotNull(result.getTariffCode());
    }
    assertEquals(firstVrn, this.responseDto.getFirstVrn());
    assertEquals(lastVrn, this.responseDto.getLastVrn());
  }

  public void responseContainsExpectedDataWithEntrantPayments(List<String> expectedVrns, 
      String firstVrn, String lastVrn) {
    List<VrnWithTariffAndEntrancesPaid> results = this.responseDto.getChargeableAccountVehicles().getResults();
    assertEquals(expectedVrns, results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    assertEquals(LocalDate.now(), results.get(0).getPaidDates().get(0));
    assertEquals(firstVrn, this.responseDto.getFirstVrn());
    assertEquals(lastVrn, this.responseDto.getLastVrn());
  }
}
