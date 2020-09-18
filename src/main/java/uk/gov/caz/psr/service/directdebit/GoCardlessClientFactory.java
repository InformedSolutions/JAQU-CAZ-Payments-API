package uk.gov.caz.psr.service.directdebit;

import com.gocardless.GoCardlessClient;
import com.gocardless.GoCardlessClient.Environment;
import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

/**
 * Factory that creates {@link GoCardlessClient}s.
 */
@Service
@Profile("!integration-tests")
@Slf4j
public class GoCardlessClientFactory extends AbstractGoCardlessClientFactory {

  private final GoCardlessClient.Environment goCardlessEnvironment;

  /**
   * Creates an instance of this class.
   */
  public GoCardlessClientFactory(@Value("${services.use.live.direct.debit.provider.client:false}")
      boolean useLiveClient,
      CredentialRetrievalManager credentialRetrievalManager) {
    super(credentialRetrievalManager);
    this.goCardlessEnvironment = useLiveClient ? Environment.LIVE : Environment.SANDBOX;
    log.debug("Using {} GoCardless environment", goCardlessEnvironment);
  }

  @Override
  public GoCardlessClient createClientFor(UUID cleanAirZoneId) {
    return GoCardlessClient.newBuilder(getAccessTokenFor(cleanAirZoneId))
        .withEnvironment(goCardlessEnvironment)
        .build();
  }

  /**
   * Returns the computed GoCardless environment. For testing purposes only.
   */
  @VisibleForTesting
  Environment getGoCardlessEnvironment() {
    return goCardlessEnvironment;
  }
}
