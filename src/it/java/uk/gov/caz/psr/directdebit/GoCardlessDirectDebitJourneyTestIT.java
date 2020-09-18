package uk.gov.caz.psr.directdebit;

import static org.hamcrest.Matchers.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import uk.gov.caz.psr.controller.DirectDebitRedirectFlowsController;
import uk.gov.caz.psr.dto.directdebit.CompleteMandateCreationRequest;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class GoCardlessDirectDebitJourneyTestIT {

  public static final String ANY_CORRELATION_ID = "007400af-abb5-4370-b50b-0ebff994741f";
  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;
  @Autowired
  private ObjectMapper objectMapper;
  @Value("${aws.direct-debit-secret-name}")
  private String apiKeySecretName;

  @LocalServerPort
  int randomServerPort;

  private static ClientAndServer govUkPayMockServer;
  private static ClientAndServer accountsServiceMockServer;

  @Nested
  class CompleteMandateCreation {

    @Test
    public void successfullyCreatedDirectDebitMandate() {
      // given
      String flowId = "RE0002W59W1ZNBRMCVWWTEVWM3B346Y3";
      String sessionToken = "3212e91fcbd19261493c909cd7a76520";
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";
      String cazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";

      mockSuccessCompleteMandateResponseInGoCardless(flowId, accountId, sessionToken, cazId);
      mockSuccessCreateMandateQueryResponseInAccounts(accountId);

      // when
      ValidatableResponse response = makeRequestToCompleteMandateCreation(flowId, sessionToken,
          cazId);

      // then
      response.statusCode(HttpStatus.OK.value());
      response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
    }

    @Nested
    class WhenPassedInvalidSessionToken {

      @Test
      public void shouldReturn400StatusCode() {
        // given
        String flowId = "RE0002W59W1ZNBRMCVWWTEVWM3B346Y3";
        String sessionToken = "3212e91fcbd19261493c909cd7a76520";
        String cazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";

        mockInvalidTokenFailureInCompleteMandateResponseInGoCardless(flowId);

        // when
        ValidatableResponse response = makeRequestToCompleteMandateCreation(flowId, sessionToken,
            cazId);

        // then
        response.statusCode(HttpStatus.BAD_REQUEST.value());
        response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
        response.body("status", equalTo(400));
        response.body("message", equalTo("The session token provided is not valid for "
            + "this redirect flow"));
      }
    }

  }

  private ValidatableResponse makeRequestToCompleteMandateCreation(String flowId,
      String sessionToken, String cazId) {
    return RestAssured.given()
        .pathParam("flowId", flowId)
        .body(toJsonString(createCompleteMandateCreationRequest(sessionToken, cazId)))
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .when()
        .post()
        .then();
  }

  private CompleteMandateCreationRequest createCompleteMandateCreationRequest(
      String sessionToken, String cazId) {
    return CompleteMandateCreationRequest.builder()
        .sessionToken(sessionToken)
        .cleanAirZoneId(cazId)
        .build();
  }

  private void mockSuccessCompleteMandateResponseInGoCardless(String flowId,
      String accountId, String sessionToken, String cazId) {
    govUkPayMockServer
        .when(HttpRequest.request()
            .withMethod("POST")
            .withPath(String.format("/redirect_flows/%s/actions/complete", flowId)))
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(
                readGoCardlessDirectDebitFile("complete-mandate-creation-response.json")
                    .replace("ACCOUNT_ID", accountId)
                    .replace("REDIRECT_FLOW_ID", flowId)
                    .replace("SESSION_TOKEN", sessionToken)
                    .replace("CAZ_ID", cazId)
            )
        );
  }

  private void mockInvalidTokenFailureInCompleteMandateResponseInGoCardless(String flowId) {
    govUkPayMockServer
        .when(HttpRequest.request()
            .withMethod("POST")
            .withPath(String.format("/redirect_flows/%s/actions/complete", flowId)))
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(
                readGoCardlessDirectDebitFile("complete-mandate-creation-error-invalid-token.json")
            )
        );
  }

  private void mockSuccessCreateMandateQueryResponseInAccounts(String accountId) {
    whenRequestToAccountsIsMadeToCreateMandate(accountId)
        .respond(HttpResponse.response().withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readInternalResponse("account-create-direct-debit-mandate-response.json")));
  }

  private ForwardChainExpectation whenRequestToAccountsIsMadeToCreateMandate(String accountId) {
    return accountsServiceMockServer
        .when(HttpRequest.request().withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withPath(String.format("/v1/accounts/%s/direct-debit-mandates", accountId)));
  }

  @BeforeEach
  public void setupRestAssuredForBasePath() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = DirectDebitRedirectFlowsController.BASE_PATH;;
  }

  @BeforeAll
  public static void startMockServers() {
    accountsServiceMockServer = startClientAndServer(1091);
    govUkPayMockServer = startClientAndServer(1080);
  }

  @AfterAll
  public static void stopMockServers() {
    govUkPayMockServer.stop();
    accountsServiceMockServer.stop();
  }

  @AfterEach
  public void resetMockServers() {
    govUkPayMockServer.reset();
    accountsServiceMockServer.reset();
  }

  @BeforeEach
  public void setApiKeyInSecretsManagerForBirminghamAndLeeds() {
    String leedsCazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
    String birminghamCazId = "53e03a28-0627-11ea-9511-ffaaee87e375";
    secretsManagerInitialisation.createSecret(apiKeySecretName,
        "test-api-key", leedsCazId, birminghamCazId);
  }

  private String readGoCardlessDirectDebitFile(String filename) {
    return readExternalFile("/directdebit/gocardless/" + filename);
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

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
