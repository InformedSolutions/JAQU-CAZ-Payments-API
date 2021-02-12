package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
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
public class VehiclePaymentHistoryControllerTestIT extends ExternalCallsIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private DataSource dataSource;

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
        .thenPageNumberIsEqualTo(0)
        .pageContainsHistoryItems(0);
  }

  @Test
  public void shouldReturn400BadRequestAndResponseWhenPageNumberIsWrong() {
    givenRequestForVrn(VRN)
        .forPageNumber(-1)
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
        .forPageNumber(0)
        .forPageSize(2)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0)
        .pageContainsHistoryItems(2);
  }

  @Test
  public void shouldReturnNoElementsWhenPageNumberTooHigh() {
    givenRequestForVrn(VRN)
        .forPageNumber(3)
        .forPageSize(2)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(3)
        .pageContainsHistoryItems(0);
  }

  @Test
  public void shouldReturnAllElementsWhenPageSizeIsBigEnough() {
    givenRequestForVrn(VRN)
        .forPageNumber(0)
        .forPageSize(10)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0)
        .pageContainsHistoryItems(5);
  }

  @Test
  public void shouldReturnElementWithAllElementsCorrectlyFilled() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0);
    EntrantPaymentEnriched firstPayment = assertion.getPayments().get(0);
    assertThat(firstPayment.getCazName()).isEqualTo("Bath");
  }

  @Test
  public void shouldReturnPaymentsSortedByTravelDateDescending() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0);
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(1).getTravelDate()).isAfter(payments.get(2).getTravelDate());
  }

  @Test
  public void shouldReturnPaymentsSortedByPaymentInsertTimestampDescending() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0);
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(0).getTravelDate()).isEqualTo(payments.get(1).getTravelDate());
    assertThat(payments.get(0).getPaymentTimestamp())
        .isAfter(payments.get(1).getPaymentTimestamp());
  }

  @Test
  public void shouldReturnMultipleItemsIfOnePaymentWasDoneForManyTravelDates() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0);
    List<EntrantPaymentEnriched> payments = assertion.getPayments();
    assertThat(payments.get(2).getPaymentTimestamp())
        .isEqualTo(payments.get(3).getPaymentTimestamp());
    assertThat(payments.get(3).getPaymentTimestamp())
        .isEqualTo(payments.get(4).getPaymentTimestamp());
  }

  @Test
  public void shouldReturnValidJson() {
    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(
        VRN)
        .forPageNumber(0)
        .forPageSize(5)
        .whenRequestForHistoryIsMade();
    ValidatableResponse response = assertion.getResponse();
    response
        .body("page", equalTo(0))
        .body("pageCount", equalTo(1))
        .body("perPage", equalTo(5))
        .body("totalPaymentsCount", equalTo(5))
        .body("payments[4].travelDate", equalTo("2019-11-01"))
        .body("payments[4].paymentTimestamp", equalTo("2020-07-01T10:00:00Z"))
        .body("payments[4].operatorId", equalTo("d47bcc60-dafc-11ea-87d0-0242ac130002"))
        .body("payments[4].cazName", equalTo("Bath"))
        .body("payments[4].paymentId", equalTo("b71b72a5-902f-4a16-a91d-1a4463b801db"))
        .body("payments[4].paymentReference", equalTo(1))
        .body("payments[4].paymentProviderStatus", equalTo("SUCCESS"))
        .body("payments[4].isRefunded", equalTo(false))
        .body("payments[4].isChargedback", equalTo(false));
  }

  @Test
  public void shouldSetIsRefundedFlagWhenPaymentWasRefundedByLA() {
    executeSqlFrom("data/sql/perform-la-update-to-refunded.sql");

    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(VRN)
        .whenRequestForHistoryIsMade();
    ValidatableResponse response = assertion.getResponse();
    response
        .body("payments[4].isRefunded", equalTo(true))
        .body("payments[4].isChargedback", equalTo(false));
  }

  @Test
  public void shouldSetIsChargedBackFlagWhenPaymentWasChargedBackByLA() {
    executeSqlFrom("data/sql/perform-la-update-to-charged-back.sql");

    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(VRN)
        .whenRequestForHistoryIsMade();
    ValidatableResponse response = assertion.getResponse();
    response
        .body("payments[4].isRefunded", equalTo(false))
        .body("payments[4].isChargedback", equalTo(true));
  }

  @Test
  public void shouldSetIsChargedBackAndIsRefundedFlagsWhenPaymentWasChargedBackAndRefundedByLA() {
    executeSqlFrom("data/sql/perform-la-update-to-refunded.sql");
    executeSqlFrom("data/sql/perform-la-update-to-charged-back.sql");

    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn(VRN)
        .whenRequestForHistoryIsMade();
    ValidatableResponse response = assertion.getResponse();
    response
        .body("payments[4].isRefunded", equalTo(true))
        .body("payments[4].isChargedback", equalTo(true));
  }

  @Test
  public void shouldNotSetLAFlagsWhenInvalidVrnIsProvided() {
    executeSqlFrom("data/sql/perform-la-update-to-refunded.sql");
    executeSqlFrom("data/sql/perform-la-update-to-charged-back.sql");

    VehiclePaymentHistoryJourneyAssertion assertion = givenRequestForVrn("ND84VSY")
        .whenRequestForHistoryIsMade();
    ValidatableResponse response = assertion.getResponse();
    response
        .body("payments[0].isRefunded", equalTo(false))
        .body("payments[0].isChargedback", equalTo(false));
  }

  @Test
  public void shouldNotThrowNpeWhenCazIdIsWrong() {
    givenRequestForVrn("ND84VSY")
        .whenRequestForHistoryIsMade()
        .thenPageNumberIsEqualTo(0)
        .pageContainsHistoryItems(1);
  }

  private VehiclePaymentHistoryJourneyAssertion givenRequestForVrn(String vrn) {
    return new VehiclePaymentHistoryJourneyAssertion().forVrn(vrn);
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }
}
