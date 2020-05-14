package uk.gov.caz.psr.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

@ExtendWith(MockitoExtension.class)
public class CredentialRetrievalManagerTest {

  private CredentialRetrievalManager credentialRetrievalManager;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private AWSSecretsManager client;

  @BeforeEach
  void init() {
    credentialRetrievalManager =
        new CredentialRetrievalManager(client, objectMapper, "testSecretName", "dd");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "testApiKey",
      " testApiKey",
      "testApiKey ",
      " testApiKey   ",
  })
  void canGetApiKeyWithOptionalWhitespaces(String apiKey) throws JsonProcessingException {
    UUID cleanAirZoneId = UUID.randomUUID();

    ObjectNode node = objectMapper.createObjectNode();
    node.put(cleanAirZoneId.toString().replace("-", ""), apiKey);

    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString())
        .thenReturn(objectMapper.writeValueAsString(node));

    Optional<String> result = credentialRetrievalManager.getCardApiKey(cleanAirZoneId);

    assertTrue(result.isPresent());
    assertThat(result).contains("testApiKey");
  }

  @Test
  void cannotGetApiKey() throws JsonProcessingException {

    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn("{}");

    Optional<String> apiKey = credentialRetrievalManager.getCardApiKey(UUID.randomUUID());

    assertThat(apiKey).isEmpty();
  }

  @Test
  void returnEmptyIfSecretStringCannotBeProcessed() throws JsonProcessingException {
    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn("{");

    Optional<String> apiKey = credentialRetrievalManager.getCardApiKey(UUID.randomUUID());

    assertThat(apiKey).isEmpty();
  }

  @Test
  void getApiKeyFromSecretBinaryString() throws JsonProcessingException {
    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn(null);

    UUID cleanAirZoneId = UUID.fromString("105db9f8-cdd0-4b0c-b906-29ce979fdc29");
    ObjectNode node = objectMapper.createObjectNode();
    node.put(cleanAirZoneId.toString().replace("-", ""), "testApiKey");
    Mockito.when(getSecretValueResponse.getSecretBinary()).thenReturn(ByteBuffer.wrap("eyIxMDVkYjlmOGNkZDA0YjBjYjkwNjI5Y2U5NzlmZGMyOSI6ICJ0ZXN0QXBpS2V5In0=".getBytes()));

    Optional<String> apiKey = credentialRetrievalManager.getCardApiKey(cleanAirZoneId);

    assertThat(apiKey).isPresent();
    assertThat(apiKey).contains("testApiKey");
  }
}
