package uk.gov.caz.psr.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.controller.AccountsController;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.PaidPaymentsResponse.PaidPaymentsResult;

@RequiredArgsConstructor
public class RetrieveChargeableAccountVehiclesJourneyAssertion {
  
  private String accountId;
  private String direction;
  private String pageSize;
  private String vrn;
  private String cleanAirZoneId;
  private ValidatableResponse response;
  private ChargeableAccountVehicleResponse responseDto;
  private static final String CORRELATION_ID = UUID.randomUUID().toString();

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
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
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
  
  public RetrieveChargeableAccountVehiclesJourneyAssertion then() {
    return this;
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    this.responseDto = response.statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID).extract()
        .as(ChargeableAccountVehicleResponse.class);
    return this;
  }

  public void responseContainsExpectedData(List<String> expectedVrns, 
      String firstVrn, String lastVrn) {
    List<PaidPaymentsResult> results = this.responseDto.getPaidPayments().getResults();
    assertTrue(this.responseDto.getPaidPayments().getResults().size() <= Integer.parseInt(this.pageSize));
    assertEquals(expectedVrns, results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    assertEquals(firstVrn, this.responseDto.getFirstVrn());
    assertEquals(lastVrn, this.responseDto.getLastVrn());
  }

  public void responseContainsExpectedDataWithEntrantPayments(List<String> expectedVrns, 
      String firstVrn, String lastVrn) {
    List<PaidPaymentsResult> results = this.responseDto.getPaidPayments().getResults();
    assertEquals(expectedVrns, results.stream().map(result -> result.getVrn()).collect(Collectors.toList()));
    assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), results.get(0).getPaidDates().get(0));
    assertEquals(this.responseDto.getFirstVrn(), firstVrn);
    assertEquals(this.responseDto.getLastVrn(), lastVrn);
  }
}
