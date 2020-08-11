package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.VehiclePaymentHistoryController;
import uk.gov.caz.psr.journeys.VehiclePaymentHistoryJourneyAssertion;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;

@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/add-caz-entrant-payments-unsorted.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@FullyRunningServerIntegrationTest
public class VehiclePaymentHistoryControllerTestIT  extends ExternalCallsIT  {

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = VehiclePaymentHistoryController.BASE_PATH;

    mockVccsCleanAirZonesCall();
  }
  private static final String VRN = "ND84VSX";

  private static final String NON_EXISTING_VRN = "ND84VSX1";

  @Test
  public void shouldReturnNoElementsWhenVehicleDoesNotExist() {
    givenRequestForVrn(NON_EXISTING_VRN)
        .whenRequestForHistoryIsMade()
        .pageContainsHistoryItems(0);
  }

  @Test
  public void shouldReturn400BadRequestAndResponseWhenPageNumberIsWrong() {
    givenRequestForVrn(VRN)
        .forPageNumber(0)
        .whenRequestForHistoryIsMade()
        .badRequest400responseIsReturned();
  }

  @Test
  public void shouldReturn400BadRequestAndResponseWhenPageSizeIsWrong() {
    givenRequestForVrn(VRN)
        .forPageSize(-1)
        .whenRequestForHistoryIsMade()
        .badRequest400responseIsReturned();
  }

  @Test
  public void shouldReturnFirstPageOfResults() {
    givenRequestForVrn(VRN)
        .forPageNumber(1)
        .forPageSize(2)
        .whenRequestForHistoryIsMade()
        .pageContainsHistoryItems(2);
  }

  @Test
  public void shouldReturnNoElementsWhenPageNumberTooHigh() {
    givenRequestForVrn(VRN)
        .forPageNumber(4)
        .forPageSize(2)
        .whenRequestForHistoryIsMade()
        .pageContainsHistoryItems(0);
  }

  @Test
  public void shouldReturnAllElementsWhenPageSizeIsBigEnough() {
    givenRequestForVrn(VRN)
        .forPageNumber(1)
        .forPageSize(10)
        .whenRequestForHistoryIsMade()
        .pageContainsHistoryItems(5);
  }

  @Test
  public void shouldReturnElementWithAllElementsCorrectlyFilled() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade();
    EntrantPaymentEnriched firstPayment = assertion.getPayments().get(0);
    assertThat(firstPayment.getCazName()).isEqualTo("Bath");
  }

  @Test
  public void shouldReturnPaymentsSortedByTravelDate() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade();
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(1).getTravelDate()).isBefore(payments.get(2).getTravelDate());
  }

  @Test
  public void shouldReturnPaymentsSortedByPaymentInsertTimestamp() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade();
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(3).getTravelDate()).isEqualTo(payments.get(4).getTravelDate());
    assertThat(payments.get(3).getPaymentTimestamp()).isBefore(payments.get(4).getPaymentTimestamp());
  }

  @Test
  public void shouldReturnMultipleItemsIfOnePaymentWasDoneForManyTravelDates() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade();
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(0).getPaymentTimestamp())
        .isEqualTo(payments.get(1).getPaymentTimestamp());
    assertThat(payments.get(1).getPaymentTimestamp())
        .isEqualTo(payments.get(2).getPaymentTimestamp());
  }

  @Test
  public void shouldNotThrowNpeWhenCazIdIsWrong() {
    givenRequestForVrn(
        "ND84VSY")
        .whenRequestForHistoryIsMade()
        .pageContainsHistoryItems(1);
  }

  private VehiclePaymentHistoryJourneyAssertion givenRequestForVrn(String vrn) {
    return new VehiclePaymentHistoryJourneyAssertion().forVrn(vrn);
  }







}
