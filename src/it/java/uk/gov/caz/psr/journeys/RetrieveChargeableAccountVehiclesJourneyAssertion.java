package uk.gov.caz.psr.journeys;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
  private String pageNumber;
  private String pageSize;
  private String cleanAirZoneId;
  private String query;
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

  public RetrieveChargeableAccountVehiclesJourneyAssertion forPage(String pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forQuery(String query) {
    this.query = query;
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion forCleanAirZoneId(
      String cleanAirZoneId) {
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
        .queryParam("pageNumber", this.pageNumber)
        .queryParam("pageSize", this.pageSize)
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

  public void responseContainsExpectedData(List<String> expectedVrns, long expectedVehiclesCount,
      int expectedPageCount, boolean expectedAnyUndeterminedVehicles) {
    List<VrnWithTariffAndEntrancesPaid> results = this.responseDto.getChargeableAccountVehicles()
        .getResults();
    assertTrue(this.responseDto.getChargeableAccountVehicles().getResults().size() <= Integer
        .parseInt(this.pageSize));
    assertEquals(expectedVrns,
        results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    assertEquals(expectedVehiclesCount, this.responseDto.getTotalVehiclesCount());
    assertEquals(expectedPageCount, this.responseDto.getPageCount());
    assertEquals(expectedAnyUndeterminedVehicles, this.responseDto.isAnyUndeterminedVehicles());
  }

  public void responseContainsExpectedDataWithEntrantPayments(List<String> expectedVrns,
      long expectedVehiclesCount, int expectedPageCount) {
    List<VrnWithTariffAndEntrancesPaid> results = this.responseDto.getChargeableAccountVehicles()
        .getResults();
    assertEquals(expectedVrns,
        results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    assertEquals(LocalDate.now(), results.get(0).getPaidDates().get(0));
    assertEquals(expectedVehiclesCount, this.responseDto.getTotalVehiclesCount());
    assertEquals(expectedPageCount, this.responseDto.getPageCount());
  }
}
