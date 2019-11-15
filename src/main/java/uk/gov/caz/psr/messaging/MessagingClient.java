package uk.gov.caz.psr.messaging;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;

/**
 * A wrapper class for an external queuing system with the ability to publish messages to the queue.
 *
 */
@Component
@Slf4j
public class MessagingClient {

  @Value("${services.sqs.new-queue-url}")
  String newQueueUrl;

  @Value("${services.sqs.message-group-id-payments}")
  String messageGroupId;

  private final AmazonSQSAsync client;
  private final ObjectMapper objectMapper;

  /**
   * A dependency injection constructor for MessagingClient.
   * 
   * @param messageGroupId the identifier for messages of this type
   * @param newQueueUrl the unique locator of the "new" queue
   * @param client the client to interface with Amazon SQS (external messaging provider)
   * @param objectMapper a mapper to convert SendEmailRequest objects to strings
   */
  public MessagingClient(@Value("${services.sqs.message-group-id-payments}") String messageGroupId,
      @Value("${services.sqs.new-queue-url}") String newQueueUrl, AmazonSQSAsync client,
      ObjectMapper objectMapper) {
    this.messageGroupId = messageGroupId;
    this.newQueueUrl = newQueueUrl;
    this.client = client;
    this.objectMapper = objectMapper;
  }

  /**
   * A method to publish a message to a queue.
   * 
   * @param message the message to be published
   * @throws JsonProcessingException thrown if the message is unable to be processed into a string
   */
  public void publishMessage(SendEmailRequest message) throws JsonProcessingException {
    log.info("Publishing message with reference number: {}", message.getReference());
    SendMessageRequest sendMessageRequest = new SendMessageRequest();
    UUID messageDeduplicationId = UUID.randomUUID();

    sendMessageRequest.setQueueUrl(newQueueUrl);
    sendMessageRequest.setMessageBody(objectMapper.writeValueAsString(message));
    sendMessageRequest.setMessageGroupId(messageGroupId);
    sendMessageRequest.setMessageDeduplicationId(messageDeduplicationId.toString());
    sendMessageRequest.putCustomRequestHeader("contentType", "application/json");

    log.info("Sending email message object to SQS with de-duplication ID: {}",
        messageDeduplicationId);
    client.sendMessage(sendMessageRequest);
  }

}
