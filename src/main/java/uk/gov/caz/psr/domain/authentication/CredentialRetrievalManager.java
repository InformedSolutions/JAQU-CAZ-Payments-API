package uk.gov.caz.psr.domain.authentication;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Class for managing the external repository of secrets used by the application.
 */
@Slf4j
@Service
public class CredentialRetrievalManager {

  String secretPrefix;
  String secretName;
  String secretProfileSeparator;
  String secretEnvironment;

  private final AWSSecretsManager client;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for manager class of the external secrets repository.
   * 
   * @param secretPrefix the prefix of the secret
   * @param secretName the name of the secret
   * @param secretProfileSeparator the profile separator of the secret
   * @param secretEnvironment the environment of the secret
   * @param awsSecretsManager an instance of {@link AWSSecretsManager}
   * @param objectMapper an instance of {@link ObjectMapper}
   */
  public CredentialRetrievalManager(@Value("${aws.secretsmanager.prefix}") String secretPrefix,
      @Value("${aws.secretsmanager.name}") String secretName,
      @Value("${aws.secretsmanager.profileSeparator}") String secretProfileSeparator,
      @Value("${aws.secretsmanager.environment}") String secretEnvironment,
      AWSSecretsManager awsSecretsManager, ObjectMapper objectMapper) {
    this.secretPrefix = secretPrefix;
    this.secretName = secretName;
    this.secretProfileSeparator = secretProfileSeparator;
    this.secretEnvironment = secretEnvironment;
    this.client = awsSecretsManager;
    this.objectMapper = objectMapper;

  }

  /**
   * Retrieve the appropriate API key given a Clean Air Zone identifier.
   * 
   * @return secret the contents of the secret
   */
  public String getSecretsValue() {
    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(generateSecretName());
    GetSecretValueResult getSecretValueResult = null;
    getSecretValueResult = client.getSecretValue(getSecretValueRequest);

    // Decrypts secret using the associated KMS CMK.
    if (getSecretValueResult.getSecretString() != null) {
      return getSecretValueResult.getSecretString();
    } else {
      return new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
    }
  }

  /**
   * Get secret value by a given key from external property source.
   * 
   * @param cleanAirZoneId the ID of the Clean Air Zone which gives the key of the secret
   * @return Secret value for a given key.
   */
  public Optional<String> getApiKey(UUID cleanAirZoneId) {

    String secretName = generateSecretKey(cleanAirZoneId);
    String rawSecretString = this.getSecretsValue();

    JsonNode jsonNode;

    try {
      jsonNode = objectMapper.readTree(rawSecretString);
      if (jsonNode.has(secretName)) {
        log.info("Successfully retrieved API key for Clean Air Zone: {}", secretName);
        String apiKey = jsonNode.get(secretName).textValue();
        return Optional.ofNullable(apiKey);
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private String generateSecretKey(UUID cleanAirZoneId) {
    String cleanAirZoneStr = cleanAirZoneId.toString();
    return cleanAirZoneStr.replace("-", "");
  }

  /**
   * Helper method to generate a full secret name from a number of variables.
   * 
   * @return the full secret name
   */
  private String generateSecretName() {
    return this.secretPrefix + "/" + this.secretName + this.secretProfileSeparator
        + this.secretEnvironment;
  }
}
