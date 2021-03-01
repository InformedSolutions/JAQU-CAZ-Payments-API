package uk.gov.caz.psr;

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
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.service.listener.PaymentReceiptSender;

@FullyRunningServerIntegrationTest
public class PaymentReceiptSenderTestIT {

  private static final String SOME_VRN = "F3FCL";
  private static final UUID BIRMINGHAM_CAZ_ID =
      UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");

  @Autowired
  PaymentReceiptSender paymentReceiptSender;

  @Autowired
  private AmazonSQS sqsClient;

  private static ClientAndServer vccsServiceMockServer;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @Value("${services.sqs.direct-debit-payment-template-id}")
  private String templateId;

  @Value("#{'${application.emails-to-skip}'.split(',')}")
  Set<String> emailsToSkip;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private String CORRECT_EMAIL = "man@informed.com";

  @BeforeAll
  public static void initServices() {
    vccsServiceMockServer = startClientAndServer(1090);
  }

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @AfterAll
  public static void tearDownServices() {
    vccsServiceMockServer.stop();
  }

  @AfterEach
  public void deleteQueue() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
  }

  @Test
  public void forOfflinePaymentSendsEmailWhenPaymentEmailIsNotSkipped() {
    Payment payment = offlinePaymentWithEmail(CORRECT_EMAIL);
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(1, payment);
    mockSuccessVccsCleanAirZonesResponse();

    paymentReceiptSender.onPaymentStatusUpdated(event);

    paymentReceiptIsSentWithTemplate(templateId);
  }

  @Test
  public void forOfflinePaymentEmailIsNotSendWhenPaymentEmailIsDefinedAsToBeSkipped() {
    String emailToBeSkipped = emailsToSkip.iterator().next();
    Payment payment = offlinePaymentWithEmail(emailToBeSkipped);
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(1, payment);
    mockSuccessVccsCleanAirZonesResponse();

    paymentReceiptSender.onPaymentStatusUpdated(event);

    paymentReceiptIsNotSent();
  }

  private Payment offlinePaymentWithEmail(String email) {
    EntrantPayment payment = EntrantPayment.builder()
        .vrn(SOME_VRN)
        .charge(10)
        .cleanAirZoneId(BIRMINGHAM_CAZ_ID)
        .travelDate(LocalDate.now())
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .updateActor(EntrantPaymentUpdateActor.USER)
        .build();
    return Payment.builder()
        .telephonePayment(true)
        .paymentMethod(PaymentMethod.DIRECT_DEBIT)
        .totalPaid(0)
        .entrantPayments(Lists.newArrayList(payment))
        .emailAddress(email)
        .externalId(RandomStringUtils.randomAlphabetic(3))
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .referenceNumber(RandomUtils.nextLong())
        .build();
  }

  private void paymentReceiptIsSentWithTemplate(String templateId) {
    Optional<Message> message = receiveSqsMessage();
    Map<String, Object> messageBody = extractToMap(message.get().getBody());
    assertThat(messageBody).containsEntry("templateId", templateId);
  }

  private void paymentReceiptIsNotSent() {
    Optional<Message> message = receiveSqsMessage();
    assertThat(message.isPresent()).isFalse();
  }

  @SneakyThrows
  private Map<String, Object> extractToMap(String json) {
    return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
    });
  }

  private Optional<Message> receiveSqsMessage() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
        queueUrlResult.getQueueUrl());
    receiveMessageRequest.withMaxNumberOfMessages(1);
    ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
    if (receiveMessageResult.getMessages().size() > 0) {
      for (Message message : receiveMessageResult.getMessages()) {
        sqsClient.deleteMessage(queueUrlResult.getQueueUrl(), message.getReceiptHandle());
      }
      return Optional.of(receiveMessageResult.getMessages().get(0));
    } else {
      return Optional.empty();
    }
  }

  private void mockSuccessVccsCleanAirZonesResponse() {
    vccsServiceMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/compliance-checker/clean-air-zones"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-clean-air-zones.json")));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
