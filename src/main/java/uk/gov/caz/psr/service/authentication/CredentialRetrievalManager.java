package uk.gov.caz.psr.service.authentication;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.SecretsManagerProperties;

/**
 * Class for managing the external repository of secrets used by the application.
 */
@Slf4j
@Service
public class CredentialRetrievalManager {

  private final AWSSecretsManager client;
  private final ObjectMapper objectMapper;
  private final String secretName;

  /**
   * Constructor for manager class of the external secrets repository.
   * 
   * @param awsSecretsManager an instance of {@link AWSSecretsManager}
   * @param objectMapper an instance of {@link ObjectMapper}
   */
  public CredentialRetrievalManager(AWSSecretsManager awsSecretsManager, ObjectMapper objectMapper,
      SecretsManagerProperties secretsManagerProperties) {
    this.client = awsSecretsManager;
    this.objectMapper = objectMapper;
    this.secretName = secretsManagerProperties.getPrefix() + "/"
        + secretsManagerProperties.getName() + secretsManagerProperties.getProfileSeparator()
        + secretsManagerProperties.getEnvironment();
  }

  /**
   * Get secret value by a given key from external property source.
   * 
   * @param cleanAirZoneId the ID of the Clean Air Zone which gives the key of the secret
   * @return Secret value for a given key.
   */
  public Optional<String> getApiKey(UUID cleanAirZoneId) {
    String generatedSecretKey = generateSecretKey(cleanAirZoneId);
    Map<String, String> secrets = this.getSecretsValue();
    
    if (secrets.containsKey(generatedSecretKey)) {
      log.info("Successfully retrieved API key for Clean Air Zone: {}", cleanAirZoneId);
      return Optional.of(secrets.get(generatedSecretKey));
    } else {
      log.error("Failed to retrieved API key for Clean Air Zone: {}", cleanAirZoneId);
      return Optional.empty();
    }
  }

  private Map<String, String> getSecretsValue() {
    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(this.secretName);
    GetSecretValueResult getSecretValueResult = null;
    getSecretValueResult = client.getSecretValue(getSecretValueRequest);

    String secretString = getSecretValueResult.getSecretString() != null
        ? getSecretValueResult.getSecretString()
        : new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());

    try {
      return objectMapper.readValue(secretString, new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      log.error("Error while parsing AWS secrets:", e);
      return Collections.emptyMap();
    }
  }
  
  private String generateSecretKey(UUID cleanAirZoneId) {
    String cleanAirZoneStr = cleanAirZoneId.toString();
    return cleanAirZoneStr.replace("-", "");
  }
}
