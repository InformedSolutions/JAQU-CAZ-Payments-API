package uk.gov.caz.psr.util;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SecretsManagerInitialisation {
  
  private final ObjectMapper objectMapper;
  private final AWSSecretsManager secretsManager;

  @SneakyThrows
  public void createSecret(String secretName) {
    String cazIdFormatted = "53e03a28-0627-11ea-9511-ffaaee87e375".replace("-", "");
    String secretString = objectMapper.writeValueAsString(
        Collections.singletonMap(cazIdFormatted, "testApiKey")
    );
    try {
      CreateSecretRequest createSecretRequest = new CreateSecretRequest()
          .withName(secretName)
          .withSecretString(secretString);
      secretsManager.createSecret(createSecretRequest);
    } catch (ResourceExistsException e) {
      PutSecretValueRequest putSecretValueRequest = new PutSecretValueRequest();
      putSecretValueRequest.withSecretId(secretName).withSecretString(secretString);
      secretsManager.putSecretValue(putSecretValueRequest);
    }
  }
}
