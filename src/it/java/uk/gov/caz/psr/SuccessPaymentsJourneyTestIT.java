package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.GetAndUpdatePaymentStatusResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
@Sql(
    scripts = {"classpath:data/sql/clear-all-payments.sql",
        "classpath:data/sql/add-vehicle-entrants.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
    scripts = {"classpath:data/sql/clear-all-payments.sql",
        "classpath:data/sql/clear-all-vehicle-entrants.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class SuccessPaymentsJourneyTestIT {

  @Value("${services.gov-uk-pay.root-url}")
  private String rootUrl;
  @Value("${services.gov-uk-pay.api-key}")
  private String apiKey;
  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private AmazonSQS sqsClient;
  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;

  private ClientAndServer mockServer;

  private final LocalDate dateWithEntityInDB = LocalDate.of(2019, 11, 2);
  private final LocalDate dateWithoutEntityInDB = LocalDate.of(2019, 11, 10);

  private String secretName = "payments/config.localstack";
  private String cazId = "53e03a28-0627-11ea-9511-ffaaee87e375";

  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/payments";
  }

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @BeforeEach
  public void createSecret() throws JsonProcessingException {
    secretsManagerInitialisation.createSecret(secretName);
  }

  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }

  @AfterEach
  public void deleteQueue() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
  }

  @Test
  public void successPaymentsJourney() {
    String externalPaymentId = "kac1ksqi26f9t2h7q3henmlamc";

    externalPaymentServiceCreatesPaymentWithId(externalPaymentId);
    andReturnsSuccessStatus();

    given().initiatePaymentRequest(initiatePaymentRequest(dateWithEntityInDB)).whenSubmitted()

        .then().paymentEntityIsCreatedInDatabase().withExternalIdEqualTo(externalPaymentId)
        .withNullPaymentAuthorisedTimestamp().andResponseIsReturnedWithMatchingInternalId()

        .and().whenRequestedToGetAndUpdateStatus()

        .then().paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .connectsEntityToPaymentIfEntityWasFound(dateWithEntityInDB)
        .doesNotConnectEntityToPaymentIfEntityWasNotFound(dateWithoutEntityInDB)
        .withNonNullPaymentAuthorisedTimestamp().andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSent();
  }

  private PaymentJourneyAssertion given() {
    return new PaymentJourneyAssertion(objectMapper, jdbcTemplate, sqsClient, emailSqsQueueName);
  }

  @RequiredArgsConstructor
  static class PaymentJourneyAssertion {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AmazonSQS sqsClient;
    private final String emailSqsQueueName;

    private InitiatePaymentRequest initiatePaymentRequest;
    private InitiatePaymentResponse initPaymentResponse;
    private GetAndUpdatePaymentStatusResponse getAndUpdatePaymentResponse;

    public PaymentJourneyAssertion initiatePaymentRequest(InitiatePaymentRequest request) {
      this.initiatePaymentRequest = request;
      return this;
    }

    public PaymentJourneyAssertion then() {
      return this;
    }

    public PaymentJourneyAssertion whenSubmitted() {
      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      this.initPaymentResponse = RestAssured.given().accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .body(toJsonString(initiatePaymentRequest)).when().post().then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.CREATED.value()).extract().response()
          .as(InitiatePaymentResponse.class);
      return this;
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }

    public PaymentJourneyAssertion paymentEntityIsCreatedInDatabase() {
      verifyThatPaymentEntityExistsWithStatus(ExternalPaymentStatus.CREATED);
      return this;
    }

    public PaymentJourneyAssertion withExternalIdEqualTo(String externalPaymentId) {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_provider_id = '" + externalPaymentId + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion andResponseIsReturnedWithMatchingInternalId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion and() {
      return this;
    }

    public PaymentJourneyAssertion whenRequestedToGetAndUpdateStatus() {
      String correlationId = "e879d028-2882-4f0b-b3b3-06d7fbcd8537";
      this.getAndUpdatePaymentResponse = RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId).when()
          .get(initPaymentResponse.getPaymentId().toString()).then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.OK.value()).extract().as(GetAndUpdatePaymentStatusResponse.class);
      return this;
    }

    public PaymentJourneyAssertion paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus status) {
      verifyThatPaymentEntityExistsWithStatus(status);
      return this;
    }

    public PaymentJourneyAssertion doesNotConnectEntityToPaymentIfEntityWasNotFound(
        LocalDate date) {
      verifyThatPaymentWasNotAssignedToEntity(date);
      return this;
    }

    public void verifyThatPaymentWasNotAssignedToEntity(LocalDate date) {
      int vehicleEntrantPaymentsCount =
          JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "vehicle_entrant_payment",
              "caz_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "' AND "
                  + "travel_date = '" + date.toString() + "' AND " + "payment_id = '"
                  + getAndUpdatePaymentResponse.getPaymentId() + "' AND "
                  + "vehicle_entrant_id is not NULL");

      assertThat(vehicleEntrantPaymentsCount).isEqualTo(0);
    }


    public PaymentJourneyAssertion connectsEntityToPaymentIfEntityWasFound(LocalDate date) {
      verifyThatPaymentWasAssignedToEntity(date);
      return this;
    }

    public void verifyThatPaymentWasAssignedToEntity(LocalDate date) {
      int vehicleEntrantPaymentsCount =
          JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "vehicle_entrant_payment",
              "caz_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "' AND "
                  + "travel_date = '" + date.toString() + "' AND " + "payment_id = '"
                  + getAndUpdatePaymentResponse.getPaymentId() + "' AND "
                  + "vehicle_entrant_id is not NULL");

      assertThat(vehicleEntrantPaymentsCount).isEqualTo(1);
    }

    private void verifyThatPaymentEntityExistsWithStatus(ExternalPaymentStatus status) {
      verifyThatPaymentExistsWithMatchingAmountAndCreditCardPaymentMethod();
      verifyThatVehicleEntrantPaymentsExistForMatchingDaysWithStatus(status);
    }

    private void verifyThatVehicleEntrantPaymentsExistForMatchingDaysWithStatus(
        ExternalPaymentStatus status) {
      int vehicleEntrantPaymentsCount =
          JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "vehicle_entrant_payment",
              "caz_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "' AND "
                  + "travel_date in (" + joinWithCommas(initiatePaymentRequest.getDays()) + ") AND "
                  + "payment_status = '" + InternalPaymentStatus.from(status).name() + "'");
      assertThat(initiatePaymentRequest.getDays()).hasSize(vehicleEntrantPaymentsCount);
    }

    private void verifyThatPaymentExistsWithMatchingAmountAndCreditCardPaymentMethod() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_method = '" + PaymentMethod.CREDIT_DEBIT_CARD.name() + "' AND " + "total_paid = "
              + initiatePaymentRequest.getAmount());
      assertThat(paymentsCount).isEqualTo(1);
    }

    private String joinWithCommas(List<LocalDate> days) {
      return Joiner.on(',').join(
          days.stream().map(date -> "'" + date.toString() + "'").collect(Collectors.toList()));
    }

    public PaymentJourneyAssertion andStatusResponseIsReturnedWithMatchinInternalId() {
      assertThat(getAndUpdatePaymentResponse.getPaymentId())
          .isEqualTo(initPaymentResponse.getPaymentId());
      return this;
    }

    public PaymentJourneyAssertion withNullPaymentAuthorisedTimestamp() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_authorised_timestamp is null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion withNonNullPaymentAuthorisedTimestamp() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_authorised_timestamp is not null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public void andPaymentReceiptIsSent() {
      List<Message> messages = receiveSqsMessages();
      assertThat(messages).isNotEmpty();
    }

    private List<Message> receiveSqsMessages() {
      GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
      ReceiveMessageResult receiveMessageResult =
          sqsClient.receiveMessage(queueUrlResult.getQueueUrl());
      return receiveMessageResult.getMessages();
    }
  }

  private InitiatePaymentRequest initiatePaymentRequest(LocalDate date) {
    return InitiatePaymentRequest.builder().amount(4200)
        .days(Arrays.asList(dateWithoutEntityInDB, dateWithEntityInDB))
        .cleanAirZoneId(UUID.fromString(this.cazId)).returnUrl("http://localhost/return-url")
        .vrn("ND84VSX").build();
  }

  private void externalPaymentServiceCreatesPaymentWithId(String externalPaymentId) {
    // externalPaymentId - not used, its value is set in
    // data/external/create-payment-response.json
    mockServer
        .when(HttpRequest.request().withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withPath(ExternalPaymentsRepository.CREATE_URI))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("create-payment-response.json")));
  }

  private void andReturnsSuccessStatus() {
    mockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())

            .withPath("/v1/payments/.*"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-payment-response.json")));
  }

  /// ----- utility methods

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
