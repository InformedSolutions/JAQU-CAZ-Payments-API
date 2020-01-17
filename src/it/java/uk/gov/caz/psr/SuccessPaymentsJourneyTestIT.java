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
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.GetAndUpdatePaymentStatusResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class SuccessPaymentsJourneyTestIT {

  private static final List<LocalDate> TRAVEL_DATES = Arrays.asList(
      LocalDate.of(2019, 11, 10),
      LocalDate.of(2019, 11, 2)
  );
  private static final String EXTERNAL_PAYMENT_ID = "kac1ksqi26f9t2h7q3henmlamc";
  private static final String CAZ_ID = "53e03a28-0627-11ea-9511-ffaaee87e375";

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;
  @Value("${aws.secret-name}")
  private String secretName;

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private AmazonSQS sqsClient;
  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;

  private ClientAndServer mockServer;

  @Test
  public void testPaymentJourneys() {
    testPaymentJourneyWithEmptyDatabase();

    testPaymentJourneyWhenEntrantPaymentsExist();

    testPaymentJourneyWhenFailedPaymentExistsExactlyForSameDays();

    testPaymentJourneyWhenFailedPaymentExistsCoversSomeDaysOfNewPayment();

    testPaymentJourneyWhenPendingPaymentExistsExactlyForSameDays();

    testPaymentJourneyWhenPaidPaymentExistsExactlyForSameDays();
  }

  private void testPaymentJourneyWhenPaidPaymentExistsExactlyForSameDays() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
    executeSqlFrom("data/sql/add-finished-payment-for-two-days.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpInternalServerErrorStatusCode()
        .andNoNewPaymentEntityIsCreatedInDatabase();
  }

  private void testPaymentJourneyWhenPendingPaymentExistsExactlyForSameDays() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
    executeSqlFrom("data/sql/add-not-finished-payment-for-two-days.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpInternalServerErrorStatusCode()
        .andNoNewPaymentEntityIsCreatedInDatabase();
  }

  private void testPaymentJourneyWhenFailedPaymentExistsCoversSomeDaysOfNewPayment() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
    executeSqlFrom("data/sql/add-failed-payment-for-three-days.sql");

    // failed payment exists for 2019-11-{10,11,12} so  the 'latest' flag is updated
    // only for 2019-11-10

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabase()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .andMatchRecordsCountWithLatestSetToFalseIsEqualTo(1)
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSent();
  }

  private void testPaymentJourneyWhenFailedPaymentExistsExactlyForSameDays() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
    executeSqlFrom("data/sql/add-failed-payment-for-two-days.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabase()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .andPreviouslyPaymentMatchRecordsHaveLatestSetToFalse()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSent();
  }

  private void testPaymentJourneyWhenEntrantPaymentsExist() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
    executeSqlFrom("data/sql/add-only-caz-entrant-payments.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabase()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .withNonNullPaymentAuthorisedTimestamp()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSent();
  }

  private void testPaymentJourneyWithEmptyDatabase() {
    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabase()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .withNonNullPaymentAuthorisedTimestamp()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSent();
  }

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

  @AfterEach
  @BeforeEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @BeforeEach
  public void mockExternalPaymentProviderResponse() {
    externalPaymentServiceCreatesPaymentWithId(EXTERNAL_PAYMENT_ID);
    andReturnsSuccessStatus();
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

    private int initialPaymentsCount = 0;
    private InitiatePaymentRequest initiatePaymentRequest;
    private InitiatePaymentResponse initPaymentResponse;
    private GetAndUpdatePaymentStatusResponse getAndUpdatePaymentResponse;
    private ValidatableResponse validatableResponse;

    public PaymentJourneyAssertion initiatePaymentRequest(InitiatePaymentRequest request) {
      this.initiatePaymentRequest = request;
      return this;
    }

    public PaymentJourneyAssertion then() {
      return this;
    }

    public PaymentJourneyAssertion whenSubmitted() {
      this.initialPaymentsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "caz_payment.t_payment");

      String correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";
      this.validatableResponse = RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .body(toJsonString(initiatePaymentRequest)).when().post().then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId);
      return this;
    }

    public PaymentJourneyAssertion expectHttpCreatedStatusCode() {
      this.initPaymentResponse = validatableResponse.statusCode(HttpStatus.CREATED.value())
          .extract()
          .response()
          .as(InitiatePaymentResponse.class);
      return this;
    }

    public PaymentJourneyAssertion expectHttpInternalServerErrorStatusCode() {
      validatableResponse.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
      return this;
    }

    @SneakyThrows
    private String toJsonString(Object request) {
      return objectMapper.writeValueAsString(request);
    }

    public PaymentJourneyAssertion paymentEntityIsCreatedInDatabase() {
      int currentPaymentsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "caz_payment.t_payment");
      assertThat(currentPaymentsCount).isGreaterThan(initialPaymentsCount);
      verifyThatPaymentEntityExistsWithStatus(ExternalPaymentStatus.CREATED);
      return this;
    }

    public PaymentJourneyAssertion andNoNewPaymentEntityIsCreatedInDatabase() {
      int currentPaymentsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "caz_payment.t_payment");
      assertThat(currentPaymentsCount).isEqualTo(initialPaymentsCount);
      return this;
    }

    public PaymentJourneyAssertion withMatchedEntrantPayments() {
      int entrantPaymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_clean_air_zone_entrant_payment_match",
          "payment_id = '" + initPaymentResponse.getPaymentId() + "' AND "
              + "latest is true");
      assertThat(entrantPaymentsCount).isEqualTo(initiatePaymentRequest.getDays().size());
      return this;
    }

    public PaymentJourneyAssertion withExternalIdEqualTo(String externalPaymentId) {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
          "payment_provider_id = '" + externalPaymentId + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion andResponseIsReturnedWithMatchingInternalId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
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

    private void verifyThatPaymentEntityExistsWithStatus(ExternalPaymentStatus status) {
      verifyThatVehicleEntrantPaymentsExistForMatchingDaysWithStatus(status);
    }

    private void verifyThatVehicleEntrantPaymentsExistForMatchingDaysWithStatus(
        ExternalPaymentStatus status) {
      int entrantPaymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment",
          "clean_air_zone_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "' AND "
              + "travel_date in (" + joinWithCommas(initiatePaymentRequest.getDays()) + ") AND "
              + "tariff_code = '" + initiatePaymentRequest.getTariffCode() + "' AND "
              + "update_actor = '" + EntrantPaymentUpdateActor.USER.name() + "' AND "
              + "payment_status = '" + InternalPaymentStatus.from(status).name() + "'");
      assertThat(entrantPaymentsCount).isEqualTo(initiatePaymentRequest.getDays().size());
    }

    public PaymentJourneyAssertion andMatchRecordsCountWithLatestSetToFalseIsEqualTo(int expectedCnt) {
      int entrantPaymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment_match",
          "latest is false");
      assertThat(entrantPaymentsCount).isEqualTo(expectedCnt);
      return this;
    }

    public PaymentJourneyAssertion andPreviouslyPaymentMatchRecordsHaveLatestSetToFalse() {
      return andMatchRecordsCountWithLatestSetToFalseIsEqualTo(initiatePaymentRequest.getDays().size());
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
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "' AND "
              + "payment_authorised_timestamp is null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion withNonNullPaymentAuthorisedTimestamp() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
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

  private InitiatePaymentRequest initiatePaymentRequest() {
    return InitiatePaymentRequest.builder()
        .amount(4200)
        .days(TRAVEL_DATES)
        .cleanAirZoneId(UUID.fromString(this.CAZ_ID))
        .returnUrl("http://localhost/return-url")
        .vrn("ND84VSX")
        .tariffCode("tariffCode")
        .build();
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
