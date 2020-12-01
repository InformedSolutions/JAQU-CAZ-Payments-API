package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import com.gocardless.GoCardlessClient;
import com.gocardless.GoCardlessClient.Environment;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

@ExtendWith(MockitoExtension.class)
class GoCardlessClientFactoryTest {

  @Mock
  private CredentialRetrievalManager credentialRetrievalManager;

  private GoCardlessClientFactory factory;

  @BeforeEach
  public void setUp() {
    factory = new GoCardlessClientFactory(false, credentialRetrievalManager);
  }

  @Nested
  class WhenUseLiveClientIsSetToFalse {

    @Test
    public void shouldReturnSandboxEnvironment() {
      // given
      factory = new GoCardlessClientFactory(false, credentialRetrievalManager);

      // when
      Environment environment = factory.getGoCardlessEnvironment();

      // then
      assertThat(environment).isEqualTo(Environment.SANDBOX);
    }

  }

  @Nested
  class WhenUseLiveClientIsSetToTrue {

    @Test
    public void shouldReturnLiveEnvironment() {
      // given
      factory = new GoCardlessClientFactory(true, credentialRetrievalManager);

      // when
      Environment environment = factory.getGoCardlessEnvironment();

      // then
      assertThat(environment).isEqualTo(Environment.LIVE);
    }
  }

  @Nested
  class WhenAccessTokenIsAbsentInSecretsManager {

    @Test
    public void shouldThrowIllegalStateException() {
      // given
      UUID cazId = UUID.randomUUID();
      mockAbsentAccessKeyFor(cazId);

      // when
      Throwable throwable = catchThrowable(() -> factory.createClientFor(cazId));

      // then
      assertThat(throwable)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Direct Debit access token has not been set");
    }

    private void mockAbsentAccessKeyFor(UUID cazId) {
      given(credentialRetrievalManager.getDirectDebitAccessToken(cazId))
          .willReturn(Optional.empty());
    }
  }

  @Test
  public void shouldCreateClient() {
    // given
    UUID cazId = UUID.randomUUID();
    given(credentialRetrievalManager.getDirectDebitAccessToken(cazId))
        .willReturn(Optional.of("access-key-1"));

    // when
    GoCardlessClient client = factory.createClientFor(cazId);

    // then
    assertThat(client).isNotNull();
  }
}