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
import com.google.common.io.Resources;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.CleanupDanglingPaymentsService;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/dangling-payments-test-data.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
    scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class DanglingPaymentsCleanupTestIT {

  private static final int EXPECTED_NON_DANGLING_PAYMENTS_COUNT = 6;
  private static final int INITIAL_DANGLING_PAYMENTS_COUNT = 3;

  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PaymentRepository paymentRepository;
  @Autowired
  private AmazonSQS sqsClient;
  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;

  @Autowired
  private CleanupDanglingPaymentsService danglingPaymentsService;

  private ClientAndServer mockServer;

  @Value("${aws.secret-name}")
  private String secretName;

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
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
  public void danglingPaymentsCleanupTest() {
    givenDanglingPaymentsWithExternalIds("cancelled-payment-id", "expired-payment-id",
        "success-payment-id");
    andNonDanglingPaymentsCountIs(EXPECTED_NON_DANGLING_PAYMENTS_COUNT);

    whenDanglingPaymentServiceIsCalled();

    thenStatusOf("cancelled-payment-id").isFailed().andStatusOfEntrantPaymentsIsNotPaid();
    // checking if expired payment will not change status of assigned EntrantPayments. 
    thenStatusOf("expired-payment-id").isFailed().andStatusOfEntrantPaymentsIsNotChanged();
    thenStatusOf("success-payment-id").isSuccess().andStatusOfEntrantPaymentsIsPaid();

    andNonDanglingPaymentsCountIs(
        EXPECTED_NON_DANGLING_PAYMENTS_COUNT + INITIAL_DANGLING_PAYMENTS_COUNT);
    andDanglingPaymentsCountIsZero();
    andPaymentReceiptIsSentOnlyOnce();
  }

  private void andPaymentReceiptIsSentOnlyOnce() {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).hasOnlyOneElementSatisfying(message -> {
      Map<String, String> body = readJsonToMap(message);
      assertThat(body.get("emailAddress")).isEqualTo("success-payment@informed.com");
      assertThat(body.get("templateId")).isEqualTo("test-template-id");
    });
  }

  @SneakyThrows
  private Map<String, String> readJsonToMap(Message message) {
    return objectMapper.readValue(message.getBody(), Map.class);
  }

  private void andDanglingPaymentsCountIsZero() {
    List<Payment> danglingPayments = paymentRepository.findDanglingPayments();
    assertThat(danglingPayments).isEmpty();
  }

  private void andNonDanglingPaymentsCountIs(int nonDanglingPaymentsCount) {
    int paymentsCount = nonDanglingPaymentsCount();
    assertThat(paymentsCount).isEqualTo(nonDanglingPaymentsCount);
  }

  private int nonDanglingPaymentsCount() {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
        "payment_provider_id IS NULL "
            + "OR payment_submitted_timestamp + INTERVAL '90 minutes' >= NOW() "
            + "OR payment_provider_status IN ('SUCCESS', 'FAILED', 'CANCELLED', 'ERROR')");
  }

  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult =
        sqsClient.receiveMessage(queueUrlResult.getQueueUrl());
    return receiveMessageResult.getMessages();
  }

  private DanglingPaymentAssertion thenStatusOf(String externalId) {
    return new DanglingPaymentAssertion(jdbcTemplate, externalId);
  }

  private void whenDanglingPaymentServiceIsCalled() {
    danglingPaymentsService.updateStatusesOfDanglingPayments();
  }

  private void givenDanglingPaymentsWithExternalIds(String cancelledPaymentExternalId,
      String expiredPaymentExternalId, String successPaymentId) {
    mockCancelledPaymentResponse(cancelledPaymentExternalId);
    mockExpiredPaymentResponse(expiredPaymentExternalId);
    mockSuccessPaymentResponse(successPaymentId);
  }

  private void mockSuccessPaymentResponse(String successPaymentId) {
    mockExternalPaymentResponse("success", successPaymentId);
  }

  private void mockCancelledPaymentResponse(String externalId) {
    mockExternalPaymentResponse("cancelled", externalId);
  }

  private void mockExpiredPaymentResponse(String externalId) {
    mockExternalPaymentResponse("expired", externalId);
  }

  private void mockExternalPaymentResponse(String prefix, String externalId) {
    mockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/payments/" + externalId))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile(prefix + "-payment.json")));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/dangling/" + filename),
        Charsets.UTF_8);
  }

  static class DanglingPaymentAssertion {

    private final JdbcTemplate jdbcTemplate;
    private final String externalId;
    private final String internalId;

    public DanglingPaymentAssertion(JdbcTemplate jdbcTemplate, String externalId) {
      this.jdbcTemplate = jdbcTemplate;
      this.externalId = externalId;
      this.internalId = findInternalIdFor(externalId);
    }

    private String findInternalIdFor(String externalId) {
      return jdbcTemplate.queryForObject(
          "select payment_id from caz_payment.t_payment where " + "payment_provider_id = '"
              + externalId + "'", String.class);
    }

    public DanglingPaymentAssertion isFailed() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_payment", "payment_provider_id = '" + externalId +
              "' " + "AND payment_provider_status = '" + ExternalPaymentStatus.FAILED.name() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public void andStatusOfEntrantPaymentsIsNotPaid() {
      // count of entries whose status is NOT equal to NOT_PAID must be zero
      int count = numberOfEntrantPaymentForPaymentIdWithNotStatus(internalId,
          InternalPaymentStatus.NOT_PAID);
      assertThat(count).isZero();
    }

    public void andStatusOfEntrantPaymentsIsNotChanged() {
      // count of entries whose status is as declared 1 not_paid and 1 refunded.
      int notPaidCount = numberOfEntrantPaymentForPaymentIdWithStatus(internalId,
          InternalPaymentStatus.NOT_PAID);
      int refundedCount = numberOfEntrantPaymentForPaymentIdWithStatus(internalId,
          InternalPaymentStatus.REFUNDED);
      assertThat(notPaidCount).isEqualTo(1);
      assertThat(refundedCount).isEqualTo(1);
    }

    public DanglingPaymentAssertion andStatusOfEntrantPaymentsIsPaid() {
      // count of entries whose status is NOT equal to PAID must be zero
      int count = numberOfEntrantPaymentForPaymentIdWithNotStatus(internalId,
          InternalPaymentStatus.PAID);
      assertThat(count).isZero();
      return this;
    }

    public DanglingPaymentAssertion isSuccess() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_payment.t_payment",
          "payment_provider_id = '" + externalId + "' " + "AND payment_provider_status = '"
              + ExternalPaymentStatus.SUCCESS.name() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    private int numberOfEntrantPaymentForPaymentIdWithStatus(String paymentId,
        InternalPaymentStatus internalPaymentStatus) {
      return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment entrant_payment " +
              "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
              + "ON entrant_payment.clean_air_zone_entrant_payment_id = "
              + "entrant_payment_match.clean_air_zone_entrant_payment_id "
              + "AND entrant_payment_match.latest IS TRUE "
              + "INNER JOIN caz_payment.t_payment payment "
              + "ON entrant_payment_match.payment_id = payment.payment_id",
          "payment.payment_id = '" + paymentId + "' "
              + "AND entrant_payment.payment_status = '"
              + internalPaymentStatus.name() + "'");
    }

    private int numberOfEntrantPaymentForPaymentIdWithNotStatus(String paymentId,
        InternalPaymentStatus internalPaymentStatus) {
      return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "caz_payment.t_clean_air_zone_entrant_payment entrant_payment " +
              "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
              + "ON entrant_payment.clean_air_zone_entrant_payment_id = "
              + "entrant_payment_match.clean_air_zone_entrant_payment_id "
              + "AND entrant_payment_match.latest IS TRUE "
              + "INNER JOIN caz_payment.t_payment payment "
              + "ON entrant_payment_match.payment_id = payment.payment_id",
          "payment.payment_id = '" + paymentId + "' "
              + "AND entrant_payment.payment_status != '"
              + internalPaymentStatus.name() + "'");
    }
  }
}
