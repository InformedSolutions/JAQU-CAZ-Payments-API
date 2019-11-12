package uk.gov.caz.psr.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;

/**
 * A wrapper class for an external queuing system with the ability to publish
 * messages to the queue.
 *
 */
@Component
@Slf4j
public class MessagingClient {

  private final QueueMessagingTemplate messagingTemplate;

  @Value("${services.sqs.new-queue-name}")
  String newQueue;

  @Value("${services.sqs.message-group-id-payments}")
  String messageGroupId;

  public MessagingClient(QueueMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * A method to publish a message to a queue.
   * 
   * @param message the message to be published
   */
  public void publishMessage(SendEmailRequest message) {
    log.info("Publishing message with reference number: {}",
        message.getReference());

    Map<String, Object> headers = new HashMap<String, Object>();
    headers.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER, messageGroupId);
    headers.put(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER,
        UUID.randomUUID().toString());
    headers.put("contentType", "application/json");

    log.info("Queue is: {}", newQueue);
    log.info("Message is: {}", message.toString());
    log.info("Headers are: {}", headers.toString());
    messagingTemplate.convertAndSend(newQueue, message, headers);
  }

}
