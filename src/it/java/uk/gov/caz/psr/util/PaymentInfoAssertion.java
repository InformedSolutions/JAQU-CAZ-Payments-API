package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static uk.gov.caz.psr.controller.ChargeSettlementController.TIMESTAMP;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.Headers;
import uk.gov.caz.psr.dto.PaymentInfoErrorsResponse;
import uk.gov.caz.psr.dto.PaymentInfoResponseV2;

public class PaymentInfoAssertion {

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static final String API_KEY_FOR_EXISTING_RECORDS = "53e03a28-0627-11ea-9511-ffaaee87e375";

  private static final String TOTAL_LINE_ITEMS_CNT = "results.collect { it.payments.collect "
      + "{ it.lineItems.size() }.sum() }.sum()";
  private static final String REFERENCE_NUMBERS_CNT = "results.payments.collect "
      + "{ it.cazPaymentReference }.findAll().flatten().size()";

  private static final String ERRORS_CNT = "errors.collect "
      + "{ it.error }.findAll().flatten().size()";

  private RequestSpecification requestSpecification = commonRequestSpecification();
  private ValidatableResponse validatableResponse;
  private PaymentInfoErrorsResponse paymentInfoErrors;
  private PaymentInfoResponseV2 paymentInfoResponseV2;

  public static PaymentInfoAssertion whenRequested() {
    return new PaymentInfoAssertion();
  }

  public PaymentInfoAssertion withParam(String key, String value) {
    requestSpecification = requestSpecification.param(key, value);
    return this;
  }

  public PaymentInfoAssertion then() {
    validatableResponse = requestSpecification.get().then();
    return this;
  }

  public PaymentInfoAssertion headerContainsCorrelationId() {
    validatableResponse = validatableResponse.header(Constants.X_CORRELATION_ID_HEADER,
        ANY_CORRELATION_ID);
    return this;
  }

  public PaymentInfoAssertion responseHasOkStatus() {
    validatableResponse = validatableResponse.statusCode(HttpStatus.OK.value());
    return this;
  }

  public PaymentInfoAssertion responseHasBadRequestStatus() {
    validatableResponse = validatableResponse.statusCode(HttpStatus.BAD_REQUEST.value());
    return this;
  }

  public PaymentInfoAssertion containsExactlyVrns(String... vrns) {
    String[] normalisedVrns = Stream.of(vrns)
        .map(AttributesNormaliser::normalizeVrn)
        .toArray(String[]::new);
    validatableResponse.body("results.vrn", hasItems(normalisedVrns));
    return this;
  }

  public PaymentInfoAssertion containsExactlyLineItemsWithTravelDates(String... travelDates) {
    validatableResponse.body("results.payments.lineItems.flatten().travelDate.toSet()",
        containsInAnyOrder(travelDates));
    return this;
  }

  public PaymentInfoAssertion containsEmptyResults() {
    validatableResponse.body("results", is(emptyIterable()));
    return this;
  }

  public PaymentInfoAssertion totalLineItemsCountIsEqualTo(int lineItemsCount) {
    validatableResponse.body(TOTAL_LINE_ITEMS_CNT, equalTo(lineItemsCount));
    return this;
  }

  public PaymentInfoAssertion containsOnePaymentWithProviderIdEqualTo(String paymentProviderId) {
    validatableResponse.body("results.collect { it.payments.findAll { it.paymentProviderId == "
        + "'" + paymentProviderId + "' } .size() }.sum()", equalTo(1));
    return this;
  }

  private RequestSpecification commonRequestSpecification() {
    return RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .header(Headers.X_API_KEY, API_KEY_FOR_EXISTING_RECORDS)
        .header(TIMESTAMP, LocalDateTime.now().toString());
  }

  public PaymentInfoAssertion doesNotContainNotPaidEntries() {
    validatableResponse.body("results.payments.lineItems.findAll { it.paymentStatus == "
        + "'notPaid' }.paymentStatus.flatten().toSet().size()", equalTo(0));
    return this;
  }

  public PaymentInfoAssertion containsReferenceNumbers() {
    validatableResponse.body(REFERENCE_NUMBERS_CNT, greaterThan(0));
    return this;
  }

  public PaymentInfoAssertion containsPages(int pages) {
    validatableResponse.body("pages", equalTo(1));
    return this;
  }

  public PaymentInfoAssertion containsErrors(int errors) {
    validatableResponse.body("errors.size()", equalTo(errors));
    paymentInfoErrors = validatableResponse.extract().body().as(PaymentInfoErrorsResponse.class);
    return this;
  }

  public PaymentInfoAssertion responseHasErrorField(String field) {
    validatableResponse.body("errors[0].field", equalTo(field));
    return this;
  }

  public PaymentInfoAssertion hasFirstResultWith(String vrn) {
    validatableResponse.body("results.vrn[0]", equalTo(vrn));
    return this;
  }

  public PaymentInfoAssertion hasSecondResultWith(String vrn) {
    validatableResponse.body("results.vrn[1]", equalTo(vrn));
    return this;
  }

  public PaymentInfoAssertion andContainsErrorWith(int errorNumber, int statusValue,
      String fieldValue, String detailValue, String titleValue) {
    assertThat(paymentInfoErrors.getErrors().get(errorNumber).getStatus()).isEqualTo(statusValue);
    assertThat(paymentInfoErrors.getErrors().get(errorNumber).getField()).isEqualTo(fieldValue);
    assertThat(paymentInfoErrors.getErrors().get(errorNumber).getDetail()).isEqualTo(detailValue);
    assertThat(paymentInfoErrors.getErrors().get(errorNumber).getTitle()).isEqualTo(titleValue);
    return this;
  }
}