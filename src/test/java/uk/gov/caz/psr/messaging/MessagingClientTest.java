package uk.gov.caz.psr.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
public class MessagingClientTest {

  private MessagingClient messagingClient;

  @Mock
  private AmazonSQS client;

  @Mock
  private ObjectMapper objectMapper;

  private SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
      .emailAddress("test@test.com")
      .personalisation("{}")
      .templateId("test-template-id")
      .build();

  @BeforeEach
  void init() {
    messagingClient = new MessagingClient("testQueue", client, objectMapper);
  }

  @Test
  void canPublishMessage() {
    // given
    messagingClient = new MessagingClient("testQueue", client, new ObjectMapper());
    mockObtainingAndSettingQueueUrl();

    // when
    messagingClient.publishMessage(sendEmailRequest);

    // then
    verify(client, times(1)).sendMessage(ArgumentMatchers.any(SendMessageRequest.class));
  }

  @Test
  public void shouldRethrowJsonProcessingException() throws JsonProcessingException {
    // given
    mockObtainingAndSettingQueueUrl();
    when(objectMapper.writeValueAsString(any())).thenThrow(new MockedJsonProcessingException(""));

    // then
    Throwable throwable = catchThrowable(() -> messagingClient.publishMessage(sendEmailRequest));

    // then
    assertThat(throwable).isInstanceOf(MockedJsonProcessingException.class);
  }

  private void mockObtainingAndSettingQueueUrl() {
    GetQueueUrlResult result = new GetQueueUrlResult();
    result.setQueueUrl("newUrl");
    when(client.getQueueUrl("testQueue")).thenReturn(result);
  }

  private static class MockedJsonProcessingException extends JsonProcessingException {

    protected MockedJsonProcessingException(String msg) {
      super(msg);
    }
  }
}
