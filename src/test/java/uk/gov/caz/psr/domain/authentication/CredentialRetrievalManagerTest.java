package uk.gov.caz.psr.domain.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

@ExtendWith(MockitoExtension.class)
public class CredentialRetrievalManagerTest {

  CredentialRetrievalManager credentialRetrievalManager;

  @Mock
  AWSSecretsManager client;

  @BeforeEach
  void init() {
    credentialRetrievalManager =
        new CredentialRetrievalManager(client, new ObjectMapper(), "testSecretName");
  }

  @Test
  void canGetApiKey() throws JsonProcessingException {
    UUID cleanAirZoneId = UUID.randomUUID();
    ObjectMapper objectMapper = new ObjectMapper();

    ObjectNode node = objectMapper.createObjectNode();
    node.put(cleanAirZoneId.toString().replace("-", ""), "testApiKey");

    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString())
        .thenReturn(objectMapper.writeValueAsString(node));

    Optional<String> apiKey = credentialRetrievalManager.getApiKey(cleanAirZoneId);

    assertTrue(apiKey.isPresent());
    assertEquals("testApiKey", apiKey.get());
  }

  @Test
  void cannotGetApiKey() throws JsonProcessingException {

    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn("{}");

    Optional<String> apiKey = credentialRetrievalManager.getApiKey(UUID.randomUUID());

    assertTrue(!apiKey.isPresent());
  }

  @Test
  void returnEmptyIfSecretStringCannotBeProcessed() throws JsonProcessingException {

    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn("{");

    Optional<String> apiKey = credentialRetrievalManager.getApiKey(UUID.randomUUID());

    assertTrue(!apiKey.isPresent());
  }
  
  @Test
  void getApiKeyFromSecretBinaryString() throws JsonProcessingException {

    ObjectMapper objectMapper = new ObjectMapper();
    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);
    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn(null);

    UUID cleanAirZoneId = UUID.fromString("105db9f8-cdd0-4b0c-b906-29ce979fdc29");
    ObjectNode node = objectMapper.createObjectNode();
    node.put(cleanAirZoneId.toString().replace("-", ""), "testApiKey");
    Mockito.when(getSecretValueResponse.getSecretBinary()).thenReturn(ByteBuffer.wrap("eyIxMDVkYjlmOGNkZDA0YjBjYjkwNjI5Y2U5NzlmZGMyOSI6ICJ0ZXN0QXBpS2V5In0=".getBytes()));

    Optional<String> apiKey = credentialRetrievalManager.getApiKey(cleanAirZoneId);

    assertTrue(apiKey.isPresent()); 
    assertEquals("testApiKey", apiKey.get());   
  }

}