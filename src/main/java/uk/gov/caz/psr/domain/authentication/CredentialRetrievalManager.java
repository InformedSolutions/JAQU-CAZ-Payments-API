package uk.gov.caz.psr.domain.authentication;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CredentialRetrievalManager {

  @Value("${aws.secretsmanager.prefix")
  String secretPrefix;

  @Value("${aws.secretsmanager.name")
  String secretName;

  @Value("${aws.secretsmanager.profileSeparator")
  String secretProfileSeparator;

  @Value("${aws.secretsmanager.environment")
  String secretEnvironment;

  private final AWSSecretsManager client;

  /**
   * Constructor for the CredentialRetrievalManager.
   * 
   * @param awsSecretsManager a bean of type AWSSecretsManager
   */
  public CredentialRetrievalManager(AWSSecretsManager awsSecretsManager) {
    this.client = awsSecretsManager;
  }

  /**
   * Retrieve the appropriate API key given a Clean Air Zone identifier.
   * 
   * @return secret the contents of the secret
   */
  public String getSecretsValue() {
    String secret;
    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(generateSecretName());
    GetSecretValueResult getSecretValueResult = null;

    getSecretValueResult = client.getSecretValue(getSecretValueRequest);

    // Decrypts secret using the associated KMS CMK.
    // Depending on whether the secret is a string or binary, one of these
    // fields will be populated.
    if (getSecretValueResult.getSecretString() != null) {
      secret = getSecretValueResult.getSecretString();
    } else {
      secret = new String(Base64.getDecoder()
          .decode(getSecretValueResult.getSecretBinary()).array());
    }
    log.info(secret);
    return secret;
  }

  /**
   * Get secret value by a given key from external property source.
   * 
   * @param  cleanAirZoneName the name to match to a Secrets value
   * @return                  Secrets value for a given key.
   */
  public Optional<String> getApiKey(String cleanAirZoneName) {
    log.info("Getting API key for Clean Air Zone: {}", cleanAirZoneName);
    String rawSecretString = this.getSecretsValue();

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode jsonNode;

    try {
      jsonNode = objectMapper.readTree(rawSecretString);
      if (jsonNode.has(cleanAirZoneName)) {
        log.info("Successfully retrieved API key for Clean Air Zone: {}",
            cleanAirZoneName);
        String apiKey = jsonNode.get(cleanAirZoneName).toString();
        return Optional.ofNullable(apiKey);
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * Helper method to generate a full secret name from a number of variables.
   * 
   * @return the full secret name
   */
  private String generateSecretName() {
    return this.secretPrefix + "/" + this.secretName
        + this.secretProfileSeparator + this.secretEnvironment;
  }
}
