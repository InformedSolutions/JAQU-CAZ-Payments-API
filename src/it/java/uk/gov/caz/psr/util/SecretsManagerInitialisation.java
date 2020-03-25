package uk.gov.caz.psr.util;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SecretsManagerInitialisation {
  
  private final ObjectMapper objectMapper;
  private final AWSSecretsManager secretsManager;

  @SneakyThrows
  public void createSecret(String secretName, String ...cleanAirZoneId) {
    Map<String, String> apiKeysMap = Arrays.asList(cleanAirZoneId)
        .stream()
        .map(cazId -> cazId.replace("-", ""))
        .collect(Collectors.toMap(Function.identity(), string -> "testApiKey"));
    String secretString = objectMapper.writeValueAsString(apiKeysMap);
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
