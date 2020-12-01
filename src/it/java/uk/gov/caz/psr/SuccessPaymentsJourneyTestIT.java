package uk.gov.caz.psr;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.StringUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentRequest.Transaction;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.ReconcilePaymentResponse;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.ExternalCardPaymentsRepository;
import uk.gov.caz.psr.service.receipt.CustomPaymentReceiptEmailCreator;
import uk.gov.caz.psr.util.AuditTableWrapper;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class SuccessPaymentsJourneyTestIT extends ExternalCallsIT {

  private static final List<LocalDate> TRAVEL_DATES = Arrays.asList(
      LocalDate.of(2019, 11, 10),
      LocalDate.of(2019, 11, 2)
  );
  private static final List<String> VRNS = Arrays.asList(
      "ND84VSX",
      "DL76MWX",
      "DS98UDG"
  );
  private static final String EXTERNAL_PAYMENT_ID = "kac1ksqi26f9t2h7q3henmlamc";
  private static final String CAZ_ID = "53e03a28-0627-11ea-9511-ffaaee87e375";

  private static final String PAYMENT_TABLE = "caz_payment.t_payment";
  private static final String ENTRANT_PAYMENT_TABLE = "caz_payment.t_clean_air_zone_entrant_payment";
  public static final UUID ANY_USER_ID = UUID.fromString("2663df5a-5c71-42e7-8bca-c4b3a9313766");

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;
  @Value("${aws.secret-name}")
  private String secretName;
  @Value("${services.sqs.account-payment-template-id}")
  private String fleetTemplateId;
  @Value("${services.sqs.template-id}")
  private String singleVehicleTemplateId;
  @Value("${services.sqs.offline-payment-id}")
  private String offlinePaymentTemplateId;

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

  private static ClientAndServer mockServer;

  @Test
  public void testPaymentJourneys() {
    mockVccsCleanAirZonesCall();

    testPaymentJourneyWithEmptyDatabase();

    testPaymentJourneyWithTelephonePaymentSetToTrueAndNonNullOperatorId();

    testPaymentJourneyWithTelephonePaymentSetToTrueAndNullOperatorId();

    testPaymentJourneyWithUserId();

    testPaymentJourneyWhenEntrantPaymentsExist();

    testPaymentJourneyWhenFailedPaymentExistsExactlyForSameDays();

    testPaymentJourneyWhenFailedPaymentExistsCoversSomeDaysOfNewPayment();

    testPaymentJourneyWhenPendingPaymentExistsExactlyForSameDays();

    testPaymentJourneyWhenPaidPaymentExistsExactlyForSameDays();
  }

  private void testPaymentJourneyWhenPaidPaymentExistsExactlyForSameDays() {
    clearAllPayments();
    executeSqlFrom("data/sql/add-finished-payment-for-two-days.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpUnprocessableEntityErrorStatusCode()
        .andNoNewPaymentEntityIsCreatedInDatabase();
  }

  private void testPaymentJourneyWhenPendingPaymentExistsExactlyForSameDays() {
    clearAllPayments();
    executeSqlFrom("data/sql/add-not-finished-payment-for-two-days.sql");

    given()
        .initiatePaymentRequest(initiatePaymentRequest())
        .whenSubmitted()

        .then()
        .expectHttpInternalServerErrorStatusCode()
        .andNoNewPaymentEntityIsCreatedInDatabase();
  }

  private void testPaymentJourneyWhenFailedPaymentExistsCoversSomeDaysOfNewPayment() {
    clearAllPayments();
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
        .andPaymentReceiptIsSentWithTemplate(singleVehicleTemplateId);
  }

  private void testPaymentJourneyWhenFailedPaymentExistsExactlyForSameDays() {
    clearAllPayments();
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
        .andMatchRecordsCountWithLatestSetToFalseIsEqualTo(2)
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSentWithTemplate(singleVehicleTemplateId);
  }

  private void testPaymentJourneyWhenEntrantPaymentsExist() {
    clearAllPayments();
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
        .andPaymentReceiptIsSentWithTemplate(singleVehicleTemplateId);
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
        .andAuditRecordsCreated()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptIsSentWithTemplate(singleVehicleTemplateId);
  }

  private void testPaymentJourneyWithTelephonePaymentSetToTrueAndNonNullOperatorId() {
    clearAllPayments();

    given()
        .initiatePaymentRequest(initiatePaymentRequestWithTrueTelephonePaymentAndNonNullOperatorId())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabaseWithTrueTelephonePayment()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .withMatchedOperatorId()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .withNonNullPaymentAuthorisedTimestamp()
        .andAuditRecordsCreated()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptForFleetIsSent(offlinePaymentTemplateId);
  }

  private void testPaymentJourneyWithTelephonePaymentSetToTrueAndNullOperatorId() {
    clearAllPayments();

    given()
        .initiatePaymentRequest(initiatePaymentRequestWithTrueTelephonePaymentAndNullOperatorId())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabaseWithTrueTelephonePayment()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .withNullOperatorId()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .withNonNullPaymentAuthorisedTimestamp()
        .andAuditRecordsCreated()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptForFleetIsSent(offlinePaymentTemplateId);
  }

  private void testPaymentJourneyWithUserId() {
    clearAllPayments();

    given()
        .initiatePaymentRequest(initiatePaymentRequestWithUserId())
        .whenSubmitted()

        .then()
        .expectHttpCreatedStatusCode()
        .paymentEntityIsCreatedInDatabaseWithUserId()
        .withExternalIdEqualTo(EXTERNAL_PAYMENT_ID)
        .withNullPaymentAuthorisedTimestamp()
        .withMatchedEntrantPayments()
        .andResponseIsReturnedWithMatchingInternalId()
        .and()
        .whenRequestedToGetAndUpdateStatus()

        .then()
        .paymentEntityStatusIsUpdatedTo(ExternalPaymentStatus.SUCCESS)
        .withNonNullPaymentAuthorisedTimestamp()
        .andAuditRecordsCreated()
        .andStatusResponseIsReturnedWithMatchinInternalId()
        .andPaymentReceiptForFleetIsSent(fleetTemplateId);
  }

  private void clearAllPayments() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  @BeforeEach
  public void setupRestAssured() {
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
  public void createSecret() {
    secretsManagerInitialisation.createSecret(secretName, "testApiKey", "53e03a28-0627-11ea-9511-ffaaee87e375");
  }

  @BeforeAll
  public static void startMockServer() {
    mockServer = startClientAndServer(1080);
  }

  @AfterAll
  public static void stopMockServer() {
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
    clearAllPayments();
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
    private ReconcilePaymentResponse reconcilePaymentResponse;
    private ValidatableResponse validatableResponse;

    public PaymentJourneyAssertion initiatePaymentRequest(InitiatePaymentRequest request) {
      this.initiatePaymentRequest = request;
      return this;
    }

    public PaymentJourneyAssertion then() {
      return this;
    }

    public PaymentJourneyAssertion whenSubmitted() {
      this.initialPaymentsCount = getCurrentPaymentsCount();

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

    public PaymentJourneyAssertion expectHttpUnprocessableEntityErrorStatusCode() {
      validatableResponse.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
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
      int currentPaymentsCount = getCurrentPaymentsCount();
      assertThat(currentPaymentsCount).isGreaterThan(initialPaymentsCount);
      verifyThatPaymentEntityExistsWithStatus(ExternalPaymentStatus.CREATED);
      return this;
    }

    private int getCurrentPaymentsCount() {
      boolean includeUserId = StringUtils.hasText(initiatePaymentRequest.getUserId());
      return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "telephone_payment is " + initiatePaymentRequest.getTelephonePayment().toString()
              + (includeUserId ? " AND user_id = '" + initiatePaymentRequest.getUserId() + "'" : ""));
    }

    public PaymentJourneyAssertion andNoNewPaymentEntityIsCreatedInDatabase() {
      int currentPaymentsCount = getCurrentPaymentsCount();
      assertThat(currentPaymentsCount).isEqualTo(initialPaymentsCount);
      return this;
    }

    public PaymentJourneyAssertion withMatchedEntrantPayments() {
      int entrantPaymentsCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_payment.t_clean_air_zone_entrant_payment_match",
              "payment_id = '" + initPaymentResponse.getPaymentId() + "' AND "
                  + "latest is true");
      assertThat(entrantPaymentsCount).isEqualTo(initiatePaymentRequest.getTransactions().size());
      return this;
    }

    public PaymentJourneyAssertion withExternalIdEqualTo(String externalPaymentId) {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_provider_id = '" + externalPaymentId + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion andResponseIsReturnedWithMatchingInternalId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion and() {
      return this;
    }

    public PaymentJourneyAssertion whenRequestedToGetAndUpdateStatus() {
      String correlationId = "e879d028-2882-4f0b-b3b3-06d7fbcd8537";
      this.reconcilePaymentResponse = RestAssured.given()
          .accept(MediaType.APPLICATION_JSON.toString())
          .contentType(MediaType.APPLICATION_JSON.toString())
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId).when()
          .put(initPaymentResponse.getPaymentId().toString()).then()
          .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
          .statusCode(HttpStatus.OK.value()).extract().as(ReconcilePaymentResponse.class);
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
          ENTRANT_PAYMENT_TABLE,
          "clean_air_zone_id = '" + initiatePaymentRequest.getCleanAirZoneId().toString() + "' AND "
              + "travel_date in (" + joinWithCommas(extractTravelDates(initiatePaymentRequest)) + ") AND "
              + "tariff_code in (" + joinWithCommas(extractTariffCodes()) + ") AND "
              + "update_actor = '" + EntrantPaymentUpdateActor.USER.name() + "' AND "
              + "payment_status = '" + InternalPaymentStatus.from(status).name() + "'");
      assertThat(entrantPaymentsCount).isEqualTo(initiatePaymentRequest.getTransactions().size());
    }

    private List<String> extractTariffCodes() {
      return initiatePaymentRequest.getTransactions().stream().map(Transaction::getTariffCode).collect(
          Collectors.toList());
    }

    private List<LocalDate> extractTravelDates(InitiatePaymentRequest initiatePaymentRequest) {
      return initiatePaymentRequest.getTransactions().stream().map(Transaction::getTravelDate)
          .collect(Collectors.toList());
    }

    public PaymentJourneyAssertion andMatchRecordsCountWithLatestSetToFalseIsEqualTo(
        int expectedCnt) {
      int entrantPaymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment_match",
          "latest is false");
      assertThat(entrantPaymentsCount).isEqualTo(expectedCnt);
      return this;
    }

    private <T> String joinWithCommas(Collection<T> collections) {
      return Joiner.on(',').join(
          collections.stream().map(object -> "'" + object + "'").collect(Collectors.toList())
      );
    }

    public PaymentJourneyAssertion andStatusResponseIsReturnedWithMatchinInternalId() {
      assertThat(reconcilePaymentResponse.getPaymentId())
          .isEqualTo(initPaymentResponse.getPaymentId());
      return this;
    }

    public PaymentJourneyAssertion withNullPaymentAuthorisedTimestamp() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "' AND "
              + "payment_authorised_timestamp is null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion withNonNullPaymentAuthorisedTimestamp() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_authorised_timestamp is not null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion andAuditRecordsCreated() {
      Set<String> vrns = initiatePaymentRequest.getTransactions().stream().map(Transaction::getVrn).collect(Collectors.toSet());
      UUID cleanAirZoneId = initiatePaymentRequest.getCleanAirZoneId();
      UUID paymentId = initPaymentResponse.getPaymentId();

      checkMasterTableWrittenToOnlyOnce(vrns, cleanAirZoneId);
      checkDetailTableWrittenToWithPaidAndNotPaidStatusForEachEntrantPayment(vrns, cleanAirZoneId);
      checkDetailTableWrittenToForPaymentStatus(paymentId, ExternalPaymentStatus.CREATED);
      checkDetailTableWrittenToForPaymentStatus(paymentId, ExternalPaymentStatus.INITIATED);
      checkDetailTableWrittenToForPaymentStatus(paymentId, ExternalPaymentStatus.SUCCESS);

      return this;
    }

    private void checkDetailTableWrittenToForPaymentStatus(UUID paymentId,
        ExternalPaymentStatus status) {
      int detailPaymentCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          AuditTableWrapper.DETAIL,
          "payment_id = '" + paymentId + "' AND payment_provider_status = '" + status.name() + "'");
      assertThat(detailPaymentCount).isEqualTo(1);
    }

    private void checkDetailTableWrittenToWithPaidAndNotPaidStatusForEachEntrantPayment(Set<String> vrns,
        UUID cleanAirZoneId) {
      Map<String, List<Transaction>> transactionsByVrn = initiatePaymentRequest.getTransactions().stream().collect(groupingBy(Transaction::getVrn));
      for (String vrn : vrns) {
        Object[] params = new Object[] {vrn, cleanAirZoneId};
        UUID masterId = jdbcTemplate.queryForObject(AuditTableWrapper.MASTER_ID_SQL, params, UUID.class);
        int detailPaymentEntrantCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
            AuditTableWrapper.DETAIL,
            AuditTableWrapper.MASTER_ID + " = '?'".replace("?", masterId.toString()));
        assertThat(detailPaymentEntrantCount).isEqualTo(transactionsByVrn.get(vrn).size() * 2);
      }
    }

    private void checkMasterTableWrittenToOnlyOnce(Set<String> vrns, UUID cleanAirZoneId) {
      int masterCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          AuditTableWrapper.MASTER, "vrn in (" + joinWithCommas(vrns) + ") AND "
              + "clean_air_zone_id = '" + cleanAirZoneId + "'");
      assertThat(masterCount).isEqualTo(vrns.size());
    }

    public void andPaymentReceiptIsSentWithTemplate(String templateId) {
      Message message = receiveSqsMessage();
      Map<String, Object> messageBody = extractToMap(message.getBody());
      Map<String, Object> personalisation = extractToMap(
          messageBody.get("personalisation").toString());
      int chargeInPennies = initiatePaymentRequest.getTransactions()
          .stream()
          .map(Transaction::getCharge)
          .reduce(0, Integer::sum);

      assertThat(messageBody).containsEntry("templateId", templateId);
      assertThat(personalisation).extracting("amount")
          .asInstanceOf(InstanceOfAssertFactories.STRING)
          .isEqualTo((chargeInPennies / 100) + ".00");
      assertThat(personalisation).extracting("date")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactlyInAnyOrderElementsOf(getTravelDatesLines());
    }

    public void andPaymentReceiptForFleetIsSent(String templateId) {
      Message message = receiveSqsMessage();
      Map<String, Object> messageBody = extractToMap(message.getBody());
      Map<String, Object> personalisation = extractToMap(
          messageBody.get("personalisation").toString());
      assertThat(messageBody).containsEntry("templateId", templateId);
      assertThat(personalisation).extracting("charges")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactlyElementsOf(getChargesLines());
    }

    private List<String> getTravelDatesLines() {
      return initiatePaymentRequest.getTransactions()
          .stream()
          .map(Transaction::getTravelDate)
          .map(travelDate -> travelDate.format(DateTimeFormatter.ofPattern(
              CustomPaymentReceiptEmailCreator.DATE_FORMAT, Locale.ENGLISH)))
          .collect(Collectors.toList());
    }

    @SneakyThrows
    private Map<String, Object> extractToMap(String json) {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private List<String> getChargesLines() {
      return initiatePaymentRequest.getTransactions()
          .stream()
          .sorted(Comparator.comparing(Transaction::getTravelDate).thenComparing(Transaction::getVrn))
          .map(this::chargeLineInFleetEmailReceipt)
          .collect(Collectors.toList());
    }

    private String chargeLineInFleetEmailReceipt(Transaction transaction) {
      return Joiner.on(" - ").join(
          transaction.getTravelDate()
              .format(DateTimeFormatter.ofPattern(CustomPaymentReceiptEmailCreator.DATE_FORMAT, Locale.ENGLISH)),
          transaction.getVrn(),
          "£" + (transaction.getCharge() / 100) + ".00"
      );
    }

    private Message receiveSqsMessage() {
      GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
      ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
          queueUrlResult.getQueueUrl());
      receiveMessageRequest.withMaxNumberOfMessages(1);
      ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
      for (Message message : receiveMessageResult.getMessages()) {
        sqsClient.deleteMessage(queueUrlResult.getQueueUrl(), message.getReceiptHandle());
      }
      return receiveMessageResult.getMessages().get(0);
    }

    public PaymentJourneyAssertion paymentEntityIsCreatedInDatabaseWithUserId() {
      paymentEntityIsCreatedInDatabase();
      return this;
    }

    public PaymentJourneyAssertion paymentEntityIsCreatedInDatabaseWithTrueTelephonePayment() {
      paymentEntityIsCreatedInDatabase();
      return this;
    }

    public PaymentJourneyAssertion andNullOperatorId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "' AND "
              + "operator_id is null");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion withMatchedOperatorId() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, PAYMENT_TABLE,
          "payment_id = '" + initPaymentResponse.getPaymentId().toString() + "' AND "
              + "operator_id = '" + initiatePaymentRequest.getOperatorId() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public PaymentJourneyAssertion withNullOperatorId() {
      return andNullOperatorId();
    }
  }

  private InitiatePaymentRequest initiatePaymentRequestWithTrueTelephonePaymentAndNonNullOperatorId() {
    return initiatePaymentRequest()
        .toBuilder()
        .telephonePayment(Boolean.TRUE)
        .operatorId(UUID.randomUUID().toString())
        .build();
  }

  private InitiatePaymentRequest initiatePaymentRequestWithTrueTelephonePaymentAndNullOperatorId() {
    return initiatePaymentRequest()
        .toBuilder()
        .telephonePayment(Boolean.TRUE)
        .operatorId(null)
        .build();
  }

  private InitiatePaymentRequest initiatePaymentRequestWithTrueTelephonePayment() {
    return initiatePaymentRequest()
        .toBuilder()
        .telephonePayment(Boolean.TRUE)
        .build();
  }

  private InitiatePaymentRequest initiatePaymentRequestWithUserId() {
    return initiatePaymentRequest()
        .toBuilder()
        .userId(ANY_USER_ID.toString())
        .build();
  }

  private InitiatePaymentRequest initiatePaymentRequest() {
    return InitiatePaymentRequest.builder()
        .telephonePayment(Boolean.FALSE)
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
        .cleanAirZoneId(UUID.fromString(CAZ_ID))
        .returnUrl("http://localhost/return-url")
        .build();
  }

  private void externalPaymentServiceCreatesPaymentWithId(String externalPaymentId) {
    // externalPaymentId - not used, its value is set in
    // data/external/create-payment-response.json
    mockServer
        .when(HttpRequest.request().withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withPath(ExternalCardPaymentsRepository.CREATE_URI))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("create-payment-response.json")));
  }

  private void andReturnsSuccessStatus() {
    mockServer.when(HttpRequest.request()
        .withMethod("GET")
        .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
        .withPath("/v1/payments/.*"))
        .respond(HttpResponse.response()
            .withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-payment-response.json")));
  }

  /// ----- utility methods

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
