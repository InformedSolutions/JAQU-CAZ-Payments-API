package uk.gov.caz.psr.messaging;

import static org.mockito.Mockito.times;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.psr.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
public class MessagingClientTest {

  @InjectMocks
  MessagingClient messagingClient;

  @Mock
  QueueMessagingTemplate messagingTemplate;

  Map<String, Object> headers;
  String message;
  SendEmailRequest sendEmailRequest;
  String emailAddress;
  String templateId;
  String personalisation;
  String reference;

  ObjectMapper om;

  @BeforeEach
  void init() throws JsonProcessingException {
    sendEmailRequest = new SendEmailRequest();
    templateId = UUID.randomUUID().toString();
    reference = UUID.randomUUID().toString();
    emailAddress = "test@test.com";
    personalisation = "{}";

    sendEmailRequest.emailAddress = emailAddress;
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = personalisation;
    sendEmailRequest.reference = reference;

    ReflectionTestUtils.setField(messagingClient, "newQueue", "new");
  }

  @Test
  void canPublishMessage() {
    messagingClient.publishMessage(sendEmailRequest);

    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("new"), ArgumentMatchers.eq(sendEmailRequest),
        Mockito.anyMap());
  }

}
