package uk.gov.caz.psr.domain.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

@ExtendWith(MockitoExtension.class)
public class CredentialRetrievalManagerTest {

  @InjectMocks
  CredentialRetrievalManager credentialRetrievalManager;

  @Mock
  AWSSecretsManager client;

  @Test
  void canGetApiKey() {
    GetSecretValueResult getSecretValueResponse = mock(GetSecretValueResult.class);

    Mockito.when(client.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResponse);
    Mockito.when(getSecretValueResponse.getSecretString()).thenReturn("testKey");

    String secret = credentialRetrievalManager.getSecretsValue();

    assertEquals("testKey", secret);
  }

}
