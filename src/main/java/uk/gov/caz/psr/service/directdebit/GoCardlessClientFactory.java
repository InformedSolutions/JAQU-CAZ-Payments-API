package uk.gov.caz.psr.service.directdebit;

import static uk.gov.caz.psr.util.Strings.mask;

import com.gocardless.GoCardlessClient;
import com.gocardless.GoCardlessClient.Environment;
import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

/**
 * Factory that creates {@link GoCardlessClient}s.
 */
@Slf4j
@Service
public class GoCardlessClientFactory {

  private final GoCardlessClient.Environment goCardlessEnvironment;
  private final CredentialRetrievalManager credentialRetrievalManager;

  /**
   * Creates an instance of this class.
   */
  public GoCardlessClientFactory(@Value("${services.use.live.direct.debit.provider.client:false}")
      boolean useLiveClient,
      CredentialRetrievalManager credentialRetrievalManager) {
    this.goCardlessEnvironment = useLiveClient ? Environment.LIVE : Environment.SANDBOX;
    this.credentialRetrievalManager = credentialRetrievalManager;
    log.debug("Using {} GoCardless environment", goCardlessEnvironment);
  }

  /**
   * Creates a new instance of {@link GoCardlessClient} dedicated for the given CAZ.
   */
  public GoCardlessClient createClientFor(UUID cleanAirZoneId) {
    return GoCardlessClient.newBuilder(getAccessTokenFor(cleanAirZoneId))
        .withEnvironment(goCardlessEnvironment)
        .build();
  }

  /**
   * Gets the access token from the AWS SM for the given CAZ. If absent, {@link
   * IllegalStateException} is thrown.
   */
  private String getAccessTokenFor(UUID cleanAirZoneId) {
    String accessToken = credentialRetrievalManager.getDirectDebitAccessToken(cleanAirZoneId)
        .orElseThrow(() -> new IllegalStateException("Direct Debit access token has not been set "
            + "for Clean Air Zone " + cleanAirZoneId));
    logMaskedAccessToken(accessToken, cleanAirZoneId);
    return accessToken;
  }

  /**
   * Logs first 3 characters of the access token.
   */
  private void logMaskedAccessToken(String accessToken, UUID cleanAirZoneId) {
    log.info("Direct Debit GoCardless access token for CAZ '{}': {}", cleanAirZoneId,
        mask(accessToken));
  }

  /**
   * Returns the computed GoCardless environment. For testing purposes only.
   */
  @VisibleForTesting
  Environment getGoCardlessEnvironment() {
    return goCardlessEnvironment;
  }
}
