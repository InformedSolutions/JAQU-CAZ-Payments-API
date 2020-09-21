package uk.gov.caz.psr.directdebit;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.gov.caz.psr.dto.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class CreateRedirectFlowJourneyTestIT {

  public static final String ANY_CORRELATION_ID = "007400af-abb5-4370-b50b-0ebff994741f";

  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${aws.direct-debit-secret-name}")
  private String apiKeySecretName;

  @LocalServerPort
  int randomServerPort;

  private static ClientAndServer goCardlessMockServer;
  private static ClientAndServer accountsServiceMockServer;

  @Test
  public void successfullyCreatedRedirectFlow() {
    // given
    String cazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
    String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";
    String returnUrl = "http://return-url.com";
    String sessionToken = "3212e91fcbd19261493c909cd7a76520";

    mockSuccessCreateRedirectFlowResponseInGoCardless(accountId, cazId, sessionToken, returnUrl);

    // when
    ValidatableResponse response = makeRequestToCreateRedirectFlow(accountId, cazId, sessionToken,
        returnUrl);

    // then
    response.statusCode(HttpStatus.CREATED.value());
    response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
  }

  private void mockSuccessCreateRedirectFlowResponseInGoCardless(String accountId,
      String cleanAirZoneId, String sessionToken, String redirectUrl) {
    goCardlessMockServer
        .when(HttpRequest.request()
            .withMethod("POST")
            .withPath(String.format("/redirect_flows")))
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(
                readGoCardlessDirectDebitFile("create-redirect-flow-response.json")
                    .replace("ACCOUNT_ID", accountId)
                    .replace("SESSION_TOKEN", sessionToken)
                    .replace("CAZ_ID", cleanAirZoneId)
                    .replace("REDIRECT_URL", redirectUrl)
            ));
  }

  private ValidatableResponse makeRequestToCreateRedirectFlow(String accountId,
      String cleanAirZoneId, String sessionToken, String returnUrl) {
    return RestAssured.given()
        .pathParam("accountId", accountId)
        .body(toJsonString(
            createDirectDebitMandateRequest(cleanAirZoneId, sessionToken, returnUrl)))
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .when()
        .post()
        .then();
  }

  private CreateDirectDebitMandateRequest createDirectDebitMandateRequest(String cleanAirZoneId,
      String sessionToken, String returnUrl) {
    return CreateDirectDebitMandateRequest.builder()
        .cleanAirZoneId(UUID.fromString(cleanAirZoneId))
        .sessionId(sessionToken)
        .returnUrl(returnUrl)
        .build();
  }

  @BeforeEach
  public void setupRestAssuredForBasePath() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = DirectDebitMandatesController.BASE_PATH;
  }

  @BeforeAll
  public static void startMockServers() {
    accountsServiceMockServer = startClientAndServer(1091);
    goCardlessMockServer = startClientAndServer(1080);
  }

  @AfterAll
  public static void stopMockServers() {
    goCardlessMockServer.stop();
    accountsServiceMockServer.stop();
  }

  @AfterEach
  public void resetMockServers() {
    goCardlessMockServer.reset();
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

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/" + filename), Charsets.UTF_8);
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
