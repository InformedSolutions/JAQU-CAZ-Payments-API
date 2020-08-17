package uk.gov.caz.psr.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.controller.VehiclePaymentHistoryController;
import uk.gov.caz.psr.dto.VehiclePaymentHistoryResponse;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;

public class VehiclePaymentHistoryJourneyAssertion {

  private String vrn;
  private int pageSize = 10;
  private int pageNumber = 0;

  private ValidatableResponse response;

  private VehiclePaymentHistoryResponse parsedResponse;

  private static final String CORRELATION_ID = UUID.randomUUID().toString();

  public List<EntrantPaymentEnriched> getPayments() {
    return parsedResponse.getPayments();
  }

  public VehiclePaymentHistoryJourneyAssertion forVrn(String vrn) {
    this.vrn = vrn;
    return this;
  }

  public VehiclePaymentHistoryJourneyAssertion forPageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public VehiclePaymentHistoryJourneyAssertion forPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  public VehiclePaymentHistoryJourneyAssertion whenRequestForHistoryIsMade() {
    this.response = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .pathParam("vrn", this.vrn)
        .queryParam("pageNumber", this.pageNumber)
        .queryParam("pageSize", this.pageSize)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get(VehiclePaymentHistoryController.GET_VEHICLE_HISTORY)
        .then();
    if (this.response.extract().statusCode() == 200) {
      this.parsedResponse = this.response
        .extract()
        .as(VehiclePaymentHistoryResponse.class);
    }
    return this;
  }

  public ValidatableResponse getResponse() {
    return this.response;
  }

  public void badRequest400responseIsReturned() {
    this.response.statusCode(400);

  }

  public void pageContainsHistoryItems(int count) {
    assertThat(this.parsedResponse.getPayments()).hasSize(count);
  }
}
