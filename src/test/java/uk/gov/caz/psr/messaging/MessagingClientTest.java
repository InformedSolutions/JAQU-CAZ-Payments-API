package uk.gov.caz.psr.messaging;

import static org.mockito.Mockito.times;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.psr.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
public class MessagingClientTest {

  MessagingClient messagingClient;

  @Mock
  AmazonSQS client;

  Map<String, Object> headers;
  String message;
  SendEmailRequest sendEmailRequest;
  String emailAddress;
  String templateId;
  String personalisation;
  String reference;

  @BeforeEach
  void init() throws JsonProcessingException {
    messagingClient = new MessagingClient("testQueue", client, new ObjectMapper());
    sendEmailRequest = SendEmailRequest.builder().emailAddress("test@test.com")
        .personalisation("{}").templateId("test-template-id").build();
    ReflectionTestUtils.setField(messagingClient, "newQueueName", "new");
  }

  @Test
  void canPublishMessage() throws JsonProcessingException {
    GetQueueUrlResult result = new GetQueueUrlResult();
    result.setQueueUrl("newUrl");
    Mockito.when(client.getQueueUrl("new")).thenReturn(result);

    messagingClient.publishMessage(sendEmailRequest);

    Mockito.verify(client, times(1)).sendMessage(ArgumentMatchers.any(SendMessageRequest.class));
  }

}
