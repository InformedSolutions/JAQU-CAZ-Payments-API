package uk.gov.caz.psr.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.DirectDebitMandatesController;
import uk.gov.caz.psr.controller.DirectDebitPaymentsController;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class DirectDebitJourneyTestIT {

  public static final String ANY_CORRELATION_ID = "007400af-abb5-4370-b50b-0ebff994741f";
  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private AmazonSQS sqsClient;
  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;
  @Value("${aws.direct-debit-secret-name}")
  private String apiKeySecretName;
  @LocalServerPort
  int randomServerPort;

  private static ClientAndServer goCardlessMockServer;
  private static ClientAndServer accountsServiceMockServer;
  private static ClientAndServer vccsServiceMockServer;

  private static final List<LocalDate> TRAVEL_DATES = Arrays.asList(
      LocalDate.of(2019, 11, 10),
      LocalDate.of(2019, 11, 2)
  );
  private static final List<String> VRNS = Arrays.asList(
      "ND84VSX",
      "DL76MWX",
      "DS98UDG"
  );

  @Nested
  class GetCleanAirZonesWithMandates {

    @Test
    public void successfullyGetCleanAirZonesWithMandates() {
      // given
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

      mockSuccessVccsCleanAirZonesResponse();
      mockSuccessMandateQueryResponseInGoCardless();
      mockSuccessMandateQueryResponseInAccounts(accountId);
      mockSuccessMandateUpdateResponse(accountId);

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
      mockSuccessMandateQueryResponseInGoCardless();
      mockSuccessMandateQueryResponseInAccounts(accountId);
      mockSuccessMandateUpdateResponse(accountId);

      // when
      ValidatableResponse response = makeRequestToGetMandates(accountId);

      // then
      response.statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    public void testWhenRequestToGoCardlessFails() {
      // given
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";

      mockSuccessVccsCleanAirZonesResponse();
      mockFailMandateQueryResponseInGoCardless();
      mockSuccessMandateQueryResponseInAccounts(accountId);
      mockSuccessMandateUpdateResponse(accountId);

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
      mockSuccessMandateQueryResponseInGoCardless();
      mockFailMandateQueryResponseInAccounts(accountId);
      mockSuccessMandateUpdateResponse(accountId);

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
      mockSuccessMandateQueryResponseInGoCardless();
      mockEmptyMandateQueryResponseInAccounts(accountId);
      mockSuccessMandateUpdateResponse(accountId);

      // when
      ValidatableResponse response = makeRequestToGetMandates(accountId);

      // then
      response.statusCode(HttpStatus.OK.value());
      response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
      response.contentType(ContentType.JSON);
      assertThat(response.extract().body().asString())
          .isEqualToIgnoringWhitespace(
              readInternalResponse("direct-debit-mandates-response-without-mandates.json"));
    }
  }

  @Nested
  class GetCleanAirZonesWithMandatesByCaz {

    @Test
    public void successfullyGetCleanAirZonesWithMandatesByCaz() {
      // given
      setupRestAssuredForGetMandatesByCazPath();
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";
      String birminghamCazId = "53e03a28-0627-11ea-9511-ffaaee87e375";

      mockSuccessMandateQueryResponseInGoCardless();

      mockSuccessMandateQueryResponseInAccounts(accountId);

      // when
      ValidatableResponse response = makeRequestToGetMandatesByCaz(accountId, birminghamCazId);

      // then
      response.statusCode(HttpStatus.OK.value());
      response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
      response.contentType(ContentType.JSON);
      assertThat(response.extract().body().asString())
          .isEqualToIgnoringWhitespace(
              readInternalResponse("direct-debit-mandates-for-caz-response.json"));
    }
  }

  @Nested
  class CreatePayment {

    @Test
    public void successfullyCreatedDirectDebitPayment() {
      // given
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";
      String mandateId = "i4nuo03jginfke5c3ebrvgig6a";
      String externalPaymentId = "u6dogn2lb0nedi1pl5i61hl41a";

      clearAllPayments();
      setupRestAssuredForCreateDirectDebitPayment();
      mockSuccessCreateDirectDebitPaymentInGoCardless(externalPaymentId);
      mockSuccessVccsCleanAirZonesResponse();

      // when
      ValidatableResponse response = makeRequestToCreateDirectDebitPayment(accountId, mandateId);

      // then
      response.statusCode(HttpStatus.CREATED.value());
      response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
      response.contentType(ContentType.JSON);
      assertThat(response.extract().body().asString()
          .contains("\"externalPaymentId\":\"" + externalPaymentId + "\""))
          .isTrue();
      assertThat(response.extract().body().asString()
          .contains("\"paymentStatus\":\"SUCCESS\""))
          .isTrue();
      verifyThatPaymentSubmittedTimestampIsNotNullFor(externalPaymentId);
    }

    @Test
    public void failedCreatedDirectDebitPayment() {
      // given
      String accountId = "36354a93-4e42-483c-ae2f-74511f6ab60e";
      String mandateId = "i4nuo03jginfke5c3ebrvgig6a";

      clearAllPayments();
      setupRestAssuredForCreateDirectDebitPayment();
      mockFailedCreateDirectDebitPaymentInGovUkPay();

      // when
      ValidatableResponse response = makeRequestToCreateDirectDebitPayment(accountId, mandateId);

      // then
      response.statusCode(HttpStatus.BAD_REQUEST.value());
      response.header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID);
      response.contentType(ContentType.JSON);
      assertThat(response.extract().body().asString()
          .contains("\"message\":\"Mandate not found\""))
          .isTrue();
    }

    private void verifyThatPaymentSubmittedTimestampIsNotNullFor(String externalPaymentId) {
      JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
          "payment_submitted_timestamp is not null "
              + "AND payment_provider_id = '" + externalPaymentId + "'");
    }

    private void clearAllPayments() {
      executeSqlFrom("data/sql/clear-all-payments.sql");
    }

    private ValidatableResponse makeRequestToCreateDirectDebitPayment(String accountId,
        String mandateId) {
      return RestAssured.given()
          .body(toJsonString(createDirectDebitPaymentRequestDto(accountId, mandateId)))
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .when()
          .post()
          .then();
    }

    private CreateDirectDebitPaymentRequest createDirectDebitPaymentRequestDto(String accountId,
        String mandateId) {
      return CreateDirectDebitPaymentRequest.builder()
          .accountId(accountId)
          .cleanAirZoneId(UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375"))
          .mandateId(mandateId)
          .userEmail("test@email.com")
          .transactions(
              VRNS.stream()
                  .flatMap(vrn -> TRAVEL_DATES.stream()
                      .map(travelDate -> Transaction.builder()
                          .charge(4200)
                          .travelDate(travelDate)
                          .vrn(vrn)
                          .tariffCode("tariffCode")
                          .build())
                  ).collect(Collectors.toList())
          )
          .build();
    }

    private void mockSuccessCreateDirectDebitPaymentInGoCardless(String externalPaymentId) {
      whenCreatePaymentRequestToGoCardlessIsMade()
          .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
              .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
              .withBody(readGoCardlessFile("create-payment-response.json")
                  .replace("PAYMENT_ID", externalPaymentId)));
    }

    private void mockFailedCreateDirectDebitPaymentInGovUkPay() {
      whenCreatePaymentRequestToGoCardlessIsMade()
          .respond(HttpResponse.response().withStatusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
              .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
              .withBody(readGoCardlessFile("create-payment-failed-response.json")));
    }

    private ForwardChainExpectation whenCreatePaymentRequestToGoCardlessIsMade() {
      return goCardlessMockServer
          .when(HttpRequest.request()
              .withMethod("POST")
              .withPath(String.format("/payments")));
    }
  }

  private ValidatableResponse makeRequestToCreateMandate(String accountId) {
    return RestAssured.given()
        .pathParam("accountId", accountId)
        .body(toJsonString(createDirectDebitMandateRequestDto(accountId)))
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .when()
        .post()
        .then();
  }

  private ValidatableResponse makeRequestToGetMandatesByCaz(String accountId,
      String cleanAirZoneId) {
    return RestAssured.given()
        .pathParam("accountId", accountId)
        .pathParam("cleanAirZoneId", cleanAirZoneId)
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .when()
        .get()
        .then();
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

  private void mockSuccessMandateQueryResponseInGoCardless() {
    whenRequestToGoCardlessIsMade()
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readGoCardlessFile("get-mandate-response.json")));
  }

  private void mockFailMandateQueryResponseInGoCardless() {
    whenRequestToGoCardlessIsMade()
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .withBody(readGoCardlessFile("invalid-api-usage-response.json")));
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

  private void mockSuccessMandateUpdateResponse(String accountId) {
    accountsServiceMockServer.when(HttpRequest.request().withMethod("PATCH")
        .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
        .withPath(String.format("/v1/accounts/%s/direct-debit-mandates", accountId)))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody("{\"message\": \"Direct debit mandates updated successfully\"}"));
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

  private ForwardChainExpectation whenRequestToGoCardlessIsMade() {
    return goCardlessMockServer
        .when(HttpRequest.request()
            .withMethod("GET")
            .withPath("/mandates/.*"));
  }

  @BeforeEach
  public void setupRestAssuredForBasePath() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = DirectDebitMandatesController.BASE_PATH;
  }

  public void setupRestAssuredForGetMandatesByCazPath() {
    RestAssured.basePath = DirectDebitMandatesController.FOR_CAZ_PATH;
  }

  public void setupRestAssuredForCreateDirectDebitPayment() {
    RestAssured.basePath = DirectDebitPaymentsController.BASE_PATH;
  }

  @BeforeAll
  public static void startMockServers() {
    accountsServiceMockServer = startClientAndServer(1091);
    goCardlessMockServer = startClientAndServer(1080);
    vccsServiceMockServer = startClientAndServer(1090);
  }

  @AfterAll
  public static void stopMockServers() {
    vccsServiceMockServer.stop();
    goCardlessMockServer.stop();
    accountsServiceMockServer.stop();
  }

  @AfterEach
  public void resetMockServers() {
    vccsServiceMockServer.reset();
    goCardlessMockServer.reset();
    accountsServiceMockServer.reset();
  }

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @AfterEach
  public void deleteQueue() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
  }

  @BeforeEach
  public void setApiKeyInSecretsManagerForBirminghamAndLeeds() {
    String leedsCazId = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
    String birminghamCazId = "53e03a28-0627-11ea-9511-ffaaee87e375";
    secretsManagerInitialisation.createSecret(apiKeySecretName, "testApiKey", leedsCazId,
        birminghamCazId);
  }

  private String readGoCardlessFile(String filename) {
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

  private CreateDirectDebitMandateRequest createDirectDebitMandateRequestDto(String accountId) {
    return CreateDirectDebitMandateRequest.builder()
        .returnUrl("http://return-url.pl")
        .cleanAirZoneId(UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375"))
        .build();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
