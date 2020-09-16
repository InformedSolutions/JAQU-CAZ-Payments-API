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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Class for managing the external repository of secrets used by the application.
 */
@Slf4j
@Service
public class CredentialRetrievalManager {

  private final AWSSecretsManager client;
  private final ObjectMapper objectMapper;
  private final String secretName;
  private final String directDebitSecretName;

  /**
   * Constructor for manager class of the external secrets repository.
   *
   * @param awsSecretsManager an instance of {@link AWSSecretsManager}
   * @param objectMapper an instance of {@link ObjectMapper}
   */
  public CredentialRetrievalManager(AWSSecretsManager awsSecretsManager, ObjectMapper objectMapper,
      @Value("${aws.secret-name}") String cardSecretName,
      @Value("${aws.direct-debit-secret-name}") String directDebitSecretName) {
    this.client = awsSecretsManager;
    this.objectMapper = objectMapper;
    this.secretName = cardSecretName;
    this.directDebitSecretName = directDebitSecretName;
  }

  /**
   * Get secret value by a given key from external property source.
   *
   * @param cleanAirZoneId the ID of the Clean Air Zone which gives the key of the secret
   * @return Secret value for a given key.
   */
  public Optional<String> getCardApiKey(UUID cleanAirZoneId) {
    return getApiKeyUsingSecret(cleanAirZoneId, secretName);
  }

  /**
   * Gets the API key for a given Direct Debit GOV.UK Pay account (each CAZ has one).
   *
   * @param cleanAirZoneId Clean Air Zone identifier.
   * @return API key wrapped in {@link Optional}.
   */
  public Optional<String> getDirectDebitApiKey(UUID cleanAirZoneId) {
    return getApiKeyUsingSecret(cleanAirZoneId, directDebitSecretName);
  }

  /**
   * Gets the access token for a given GoCardless account (each CAZ has one).
   *
   * @param cleanAirZoneId Clean Air Zone identifier.
   * @return Access token wrapped in {@link Optional}.
   */
  public Optional<String> getDirectDebitAccessToken(UUID cleanAirZoneId) {
    return getApiKeyUsingSecret(cleanAirZoneId, directDebitSecretName);
  }

  private Optional<String> getApiKeyUsingSecret(UUID cleanAirZoneId, String directDebitSecretName) {
    String generatedSecretKey = generateSecretKey(cleanAirZoneId);
    Map<String, String> secrets = getSecretsValue(directDebitSecretName);

    if (secrets.containsKey(generatedSecretKey)) {
      log.info("Successfully retrieved API key for Clean Air Zone: {}", cleanAirZoneId);
      return Optional.of(secrets.get(generatedSecretKey).trim());
    } else {
      log.error("Failed to retrieved API key for Clean Air Zone: {}", cleanAirZoneId);
      return Optional.empty();
    }
  }

  private Map<String, String> getSecretsValue(String secretName) {
    GetSecretValueResult getSecretValueResult = getGetSecretValueFor(secretName);

    // this is a sample code provided by AWS in AWS Secret Manager console view
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

  private GetSecretValueResult getGetSecretValueFor(String secretName) {
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(secretName);
    return client.getSecretValue(getSecretValueRequest);
  }

  private String generateSecretKey(UUID cleanAirZoneId) {
    String cleanAirZoneStr = cleanAirZoneId.toString();
    return cleanAirZoneStr.replace("-", "");
  }
}
