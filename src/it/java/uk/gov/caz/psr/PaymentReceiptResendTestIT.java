package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.psr.controller.PaymentReceiptController.BASE_PATH;
import static uk.gov.caz.psr.controller.PaymentReceiptController.RESEND_RECEIPT_EMAIL;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.ResendReceiptEmailRequest;

@Sql(scripts = "classpath:data/sql/add-caz-entrant-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@FullyRunningServerIntegrationTest
@AutoConfigureMockMvc
public class PaymentReceiptResendTestIT {

  private static final String ANY_CORRELATION_ID = "31f69f26-fb99-11e9-8483-9fcf0b2b434f";
  private static final String NON_EXISTING_REFERENCE_NUMBER = "2307";
  private static final String EXISTING_REFERENCE_NUMBER = "3000";
  private static final String ANY_EMAIL = "man@informed.com";

  @Autowired
  private AmazonSQS sqsClient;

  private static ClientAndServer vccsServiceMockServer;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @Value("${services.sqs.account-payment-template-id}")
  private String templateId;

  @Autowired
  private MockMvc mockMvc;

  private static final String RESEND_PATH = BASE_PATH + "/" + RESEND_RECEIPT_EMAIL;

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
  public void shouldReturn404WhenProvidedPaymentReferenceDoesNotExist() throws Exception {
    String payload = createResendReceiptsPayload(ANY_EMAIL);

    mockMvc.perform(post(RESEND_PATH, NON_EXISTING_REFERENCE_NUMBER)
        .content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment with given reference number not found"));

    paymentReceiptIsNotSent();
  }

  @Test
  public void shouldSendEmailWhenProvidedPaymentReferenceExists() throws Exception {
    mockSuccessVccsCleanAirZonesResponse();
    String payload = createResendReceiptsPayload(ANY_EMAIL);

    mockMvc.perform(post(RESEND_PATH, EXISTING_REFERENCE_NUMBER)
        .content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    paymentReceiptIsSentWithTemplateAndEmail(templateId, ANY_EMAIL);
  }

  private String createResendReceiptsPayload(String email) {
    ResendReceiptEmailRequest request = ResendReceiptEmailRequest.builder()
        .email(email)
        .build();
    return toJsonString(request);
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

  private void paymentReceiptIsNotSent() {
    Optional<Message> message = receiveSqsMessage();
    assertThat(message.isPresent()).isFalse();
  }

  private void paymentReceiptIsSentWithTemplateAndEmail(String templateId, String email) {
    Optional<Message> message = receiveSqsMessage();
    Map<String, Object> messageBody = extractToMap(message.get().getBody());
    assertThat(messageBody).containsEntry("templateId", templateId);
    assertThat(messageBody).containsEntry("emailAddress", email);
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

  @SneakyThrows
  private Map<String, Object> extractToMap(String json) {
    return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
    });
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
