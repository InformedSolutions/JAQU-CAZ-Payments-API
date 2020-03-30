package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class SecretsManagerInitialisation {
  
  private final ObjectMapper objectMapper;
  private final AWSSecretsManager secretsManager;
  
  public void createSecret(String secretName) throws JsonProcessingException {
    String cazIdFormatted = "53e03a28-0627-11ea-9511-ffaaee87e375".replace("-", "");
    ObjectNode node = objectMapper.createObjectNode();
    node.put(cazIdFormatted, "testApiKey");
    String secretString = objectMapper.writeValueAsString(node);
    log.info("Secret string is {}", secretString);

    try {
      CreateSecretRequest createSecretRequest = new CreateSecretRequest();
      createSecretRequest.setName(secretName);
      createSecretRequest.setSecretString(secretString);
      CreateSecretResult response = secretsManager.createSecret(createSecretRequest);
      log.info(response.toString());
    } catch (ResourceExistsException e) {
      PutSecretValueRequest putSecretValueRequest = new PutSecretValueRequest();
      putSecretValueRequest.withSecretId(secretName).withSecretString(secretString);
      secretsManager.putSecretValue(putSecretValueRequest);
    }

  }

}
