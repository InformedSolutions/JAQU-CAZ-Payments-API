package uk.gov.caz.psr.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.ForwardChainExpectation;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.DirectDebitMandatesController;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class GetCazWithMandatesTestIT {

  public static final String ANY_CORRELATION_ID = "007400af-abb5-4370-b50b-0ebff994741f";

  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;
  @Value("${aws.direct-debit-secret-name}")
  private String apiKeySecretName;
  @LocalServerPort
  int randomServerPort;

  private ClientAndServer govUkPayMockServer;
  private ClientAndServer accountsServiceMockServer;
  private ClientAndServer vccsServiceMockServer;

  @Test
  public void successfullyGetCleanAirZonesWithMandates() {
    // given
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

    mockSuccessVccsCleanAirZonesResponse();
    mockSuccessMandateQueryResponseInGovUkPay();
    mockSuccessMandateQueryResponseInAccounts(accountId);

    // when
    ValidatableResponse response = makeRequestToGetMandates(accountId);

    // then
    response.statusCode(HttpStatus.OK.value());
    response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
    response.contentType(ContentType.JSON);
    assertThat(response.extract().body().asString())
        .isEqualToIgnoringWhitespace(readInternalResponse("direct-debit-mandates-response.json"));
  }

  @Test
  public void testWhenRequestToVccsFails() {
    // given
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

    mockFailVccsCleanAirZonesCall();
    mockSuccessMandateQueryResponseInGovUkPay();
    mockSuccessMandateQueryResponseInAccounts(accountId);

    // when
    ValidatableResponse response = makeRequestToGetMandates(accountId);

    // then
    response.statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
  }

  @Test
  public void testWhenRequestToGovUkFails() {
    // given
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

    mockSuccessVccsCleanAirZonesResponse();
    mockFailMandateQueryResponseInGovUkPay();
    mockSuccessMandateQueryResponseInAccounts(accountId);

    // when
    ValidatableResponse response = makeRequestToGetMandates(accountId);

    // then
    response.statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
  }

  @Test
  public void testWhenRequestToGetMandateFromAccountsFails() {
    // given
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

    mockSuccessVccsCleanAirZonesResponse();
    mockSuccessMandateQueryResponseInGovUkPay();
    mockFailMandateQueryResponseInAccounts(accountId);

    // when
    ValidatableResponse response = makeRequestToGetMandates(accountId);

    // then
    response.statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
  }

  @Test
  public void testWhenMandatesFromAccountsAreEmpty() {
    // given
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

    mockSuccessVccsCleanAirZonesResponse();
    mockSuccessMandateQueryResponseInGovUkPay();
    mockEmptyMandateQueryResponseInAccounts(accountId);

    // when
    ValidatableResponse response = makeRequestToGetMandates(accountId);

    // then
    response.statusCode(HttpStatus.OK.value());
    response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
    response.contentType(ContentType.JSON);
    assertThat(response.extract().body().asString())
        .isEqualToIgnoringWhitespace(readInternalResponse("direct-debit-mandates-response-without-mandates.json"));
  }

  private void mockFailVccsCleanAirZonesCall() {
    whenRequestToVccsIsMade()
        .respond(HttpResponse.response().withStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString()));
  }

  private ValidatableResponse makeRequestToGetMandates(String accountId) {
    return RestAssured.given()
        .pathParam("accountId", accountId)
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .when()
        .get()
        .then();
  }

  private void mockSuccessVccsCleanAirZonesResponse() {
    whenRequestToVccsIsMade()
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readExternalFile("get-clean-air-zones.json")));
  }

  private void mockSuccessMandateQueryResponseInGovUkPay() {
    whenRequestToGovUkPayIsMade()
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readDirectDebitFile("get-mandate-response.json")));
  }

  private void mockFailMandateQueryResponseInGovUkPay() {
    whenRequestToGovUkPayIsMade()
        .respond(HttpResponse.response().withStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString()));
  }

  private void mockSuccessMandateQueryResponseInAccounts(String accountId) {
    whenRequestToAccountsIsMade(accountId)
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readInternalResponse("account-direct-debit-mandates-response.json")));
  }

  private void mockFailMandateQueryResponseInAccounts(String accountId) {
    whenRequestToAccountsIsMade(accountId)
        .respond(HttpResponse.response().withStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString()));
  }

  private void mockEmptyMandateQueryResponseInAccounts(String accountId) {
    whenRequestToAccountsIsMade(accountId)
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readInternalResponse("account-direct-debit-mandates-empty-response.json")));
  }

  private ForwardChainExpectation whenRequestToVccsIsMade() {
    return vccsServiceMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/compliance-checker/clean-air-zones"));
  }

  private ForwardChainExpectation whenRequestToAccountsIsMade(String accountId) {
    return accountsServiceMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath(String.format("/v1/accounts/%s/direct-debit-mandates", accountId)));
  }

  private ForwardChainExpectation whenRequestToGovUkPayIsMade() {
    return govUkPayMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/directdebit/mandates/.*"));
  }


  @BeforeEach
  public void startMockServers() {
    accountsServiceMockServer = startClientAndServer(1091);
    govUkPayMockServer = startClientAndServer(1080);
    vccsServiceMockServer = startClientAndServer(1090);
  }

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = DirectDebitMandatesController.BASE_PATH;
  }

  @AfterEach
  public void stopMockServers() {
    vccsServiceMockServer.stop();
    govUkPayMockServer.stop();
    accountsServiceMockServer.stop();
  }

  @BeforeEach
  public void setApiKeyInSecretsManagerForBirminghamAndLeeds() {
    String leedsCazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
    String birminghamCazId = "53e03a28-0627-11ea-9511-ffaaee87e375";
    secretsManagerInitialisation.createSecret(apiKeySecretName, leedsCazId,
        birminghamCazId);
  }

  private String readDirectDebitFile(String filename) {
    return readExternalFile("/directdebit/" + filename);
  }

  private String readExternalFile(String filename) {
    return readFile("/external/" + filename);
  }

  private String readInternalResponse(String filename) {
    return readFile("/json/response/" + filename);
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/" + filename), Charsets.UTF_8);
  }
}
