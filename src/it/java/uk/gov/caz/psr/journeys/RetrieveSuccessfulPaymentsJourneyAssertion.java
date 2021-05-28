package uk.gov.caz.psr.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.controller.AccountsController;
import uk.gov.caz.psr.dto.SuccessfulPaymentsResponse;

@RequiredArgsConstructor
public class RetrieveSuccessfulPaymentsJourneyAssertion {

  private String accountId;
  private String accountUserId;
  private String pageNumber;
  private String pageSize;

  private ValidatableResponse response;
  private SuccessfulPaymentsResponse successfulPaymentsResponse;

  private static final String CORRELATION_ID = UUID.randomUUID().toString();

  public RetrieveSuccessfulPaymentsJourneyAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion forAccountUserId(String accountUserId) {
    this.accountUserId = accountUserId;
    return this;
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion forPageNumber(String pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion forPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion whenRequestIsMade() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;

    this.response = prepareRequestWithParams()
        .when()
        .get("/{accountId}/payments")
        .then();

    return this;
  }

  private RequestSpecification prepareRequestWithParams() {
    RequestSpecification request = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("accountId", this.accountId)
        .queryParam("pageNumber", this.pageNumber)
        .queryParam("pageSize", this.pageSize);

    return this.accountUserId == null
        ? request
        : request.queryParam("accountUserId", this.accountUserId);
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion then() {
    return this;
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    this.successfulPaymentsResponse = response
        .statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .extract()
        .as(SuccessfulPaymentsResponse.class);

    return this;
  }

  public void responseHadNoData() {
    SuccessfulPaymentsResponse response = this.successfulPaymentsResponse;

    assertThat(response.getTotalPaymentsCount()).isEqualTo(0);
    assertThat(response.getPageCount()).isEqualTo(0);
    assertThat(response.getPayments()).isEmpty();
  }

  public void responseIncludeDataOfAllUsers() {
    SuccessfulPaymentsResponse response = this.successfulPaymentsResponse;

    assertThat(response.getTotalPaymentsCount()).isEqualTo(2);
    assertThat(getPayersNames().size()).isEqualTo(2);
  }

  public RetrieveSuccessfulPaymentsJourneyAssertion responseIncludeDataOfASingleUser() {
    SuccessfulPaymentsResponse response = this.successfulPaymentsResponse;

    assertThat(response.getTotalPaymentsCount()).isEqualTo(1);
    assertThat(getPayersNames().size()).isEqualTo(1);

    return this;
  }

  public void responseIncludeRequiredFlags() {
    SuccessfulPaymentsResponse response = this.successfulPaymentsResponse;

    assertThat(response.getPayments().stream().findFirst().get().isRefunded()).isTrue();
    assertThat(response.getPayments().stream().findFirst().get().isChargedback()).isTrue();
    assertThat(response.getPayments().stream().findFirst().get().isUnsuccessful()).isTrue();
  }

  private Set<String> getPayersNames() {
    return this.successfulPaymentsResponse
        .getPayments()
        .stream()
        .map(payment -> payment.getPayerName())
        .collect(Collectors.toSet());
  }
}
