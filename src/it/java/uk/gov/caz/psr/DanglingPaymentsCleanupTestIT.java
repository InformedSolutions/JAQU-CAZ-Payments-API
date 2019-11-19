package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
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

@IntegrationTest
@Sql(scripts = "classpath:data/sql/dangling-payments-test-data.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/clear-all-vehicle-entrants.sql"},
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
  private CleanupDanglingPaymentsService danglingPaymentsService;

  private ClientAndServer mockServer;

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
    givenDanglingPaymentsWithExternalIds(
        "cancelled-payment-id",
        "expired-payment-id",
        "success-payment-id"
    );
    andNonDanglingPaymentsCountIs(EXPECTED_NON_DANGLING_PAYMENTS_COUNT);

    whenDanglingPaymentServiceIsCalled();

    thenStatusOf("cancelled-payment-id")
        .isFailed()
        .andStatusOfVehicleEntrantPaymentsIsNotPaid();
    thenStatusOf("expired-payment-id")
        .isFailed()
        .andStatusOfVehicleEntrantPaymentsIsNotPaid();
    thenStatusOf("success-payment-id")
        .isSuccess()
        .andStatusOfVehicleEntrantPaymentsIsPaid()
        .andPaymentIsLinkedToVehicleEntrant();

    andNonDanglingPaymentsCountIs(EXPECTED_NON_DANGLING_PAYMENTS_COUNT
        + INITIAL_DANGLING_PAYMENTS_COUNT);
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
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
        "payment_provider_id IS NULL "
            + "OR payment_submitted_timestamp + INTERVAL '90 minutes' >= NOW() "
            + "OR payment_provider_status IN ('SUCCESS', 'FAILED', 'CANCELLED', 'ERROR')");
  }
  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(
        queueUrlResult.getQueueUrl());
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
    mockServer.when(
        HttpRequest.request()
            .withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/payments/" + externalId)
    ).respond(
        HttpResponse.response()
            .withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile(prefix + "-payment.json"))
    );
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(
        Resources.getResource("data/external/dangling/" + filename),
        Charsets.UTF_8
    );
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
      return jdbcTemplate.queryForObject("select payment_id from payment where "
          + "payment_provider_id = '" + externalId + "'", String.class);
    }

    public DanglingPaymentAssertion isFailed() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_provider_id = '" + externalId + "' "
              + "AND payment_provider_status = '" + ExternalPaymentStatus.FAILED.name() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public void andStatusOfVehicleEntrantPaymentsIsNotPaid() {
      // count of entries whose status is NOT equal to NOT_PAID must be zero
      int count = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment",
          "payment_id = '" + internalId + "' "
              + "AND payment_status != '" + InternalPaymentStatus.NOT_PAID.name() + "'");
      assertThat(count).isZero();
    }

    public DanglingPaymentAssertion andStatusOfVehicleEntrantPaymentsIsPaid() {
      // count of entries whose status is NOT equal to PAID must be zero
      int count = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment",
          "payment_id = '" + internalId + "' "
              + "AND payment_status != '" + InternalPaymentStatus.PAID.name() + "'");
      assertThat(count).isZero();
      return this;
    }

    public DanglingPaymentAssertion isSuccess() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_provider_id = '" + externalId + "' "
              + "AND payment_provider_status = '" + ExternalPaymentStatus.SUCCESS.name() + "'");
      assertThat(paymentsCount).isEqualTo(1);
      return this;
    }

    public DanglingPaymentAssertion andPaymentIsLinkedToVehicleEntrant() {
      int linkedVehicleEntrantsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
          "vehicle_entrant_payment", "vehicle_entrant_id is not null");
      assertThat(linkedVehicleEntrantsCount).isPositive();
      return this;
    }
  }
}
