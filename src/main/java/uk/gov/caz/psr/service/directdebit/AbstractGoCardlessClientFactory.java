package uk.gov.caz.psr.service.directdebit;

import static uk.gov.caz.psr.util.Strings.mask;

import com.gocardless.GoCardlessClient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

@AllArgsConstructor
@Slf4j
public abstract class AbstractGoCardlessClientFactory {

  private final CredentialRetrievalManager credentialRetrievalManager;

  /**
   * Creates a new instance of {@link GoCardlessClient} dedicated for the given CAZ.
   */
  public abstract GoCardlessClient createClientFor(UUID cleanAirZoneId);

  /**
   * Gets the access token from the AWS SM for the given CAZ. If absent, {@link
   * IllegalStateException} is thrown.
   */
  protected final String getAccessTokenFor(UUID cleanAirZoneId) {
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
}
