package uk.gov.caz.psr.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
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

  String newQueueName;

  private final AmazonSQS client;
  private final ObjectMapper objectMapper;

  /**
   * A dependency injection constructor for MessagingClient.
   * 
   * @param newQueueName the name of the "new" queue
   * @param client the client to interface with Amazon SQS (external messaging provider)
   * @param objectMapper a mapper to convert SendEmailRequest objects to strings
   */
  public MessagingClient(@Value("${services.sqs.new-queue-name}") String newQueueName, 
      AmazonSQS client, ObjectMapper objectMapper) {
    this.newQueueName = newQueueName;
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

    sendMessageRequest.setQueueUrl(client.getQueueUrl(newQueueName).getQueueUrl());
    sendMessageRequest.setMessageBody(objectMapper.writeValueAsString(message));
    sendMessageRequest.setMessageGroupId(UUID.randomUUID().toString());
    sendMessageRequest.setMessageDeduplicationId(messageDeduplicationId.toString());
    sendMessageRequest.putCustomRequestHeader("contentType", "application/json");

    log.info("Sending email message object to SQS queue {} with de-duplication ID: {}",
        newQueueName, messageDeduplicationId);
    client.sendMessage(sendMessageRequest);
  }

}
