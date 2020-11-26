package uk.gov.caz.psr.service.directdebit;

import com.gocardless.GoCardlessClient;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

/**
 * Creates a 'test' version of the GoCardless client. Use in integration tests only.
 */
@Service
@Profile("integration-tests")
public class TestGoCardlessClientFactory extends AbstractGoCardlessClientFactory {

  private final String rootUrl;

  public TestGoCardlessClientFactory(
      CredentialRetrievalManager credentialRetrievalManager,
      @Value("${services.go-cardless.root-url}") String rootUrl) {
    super(credentialRetrievalManager);
    this.rootUrl = rootUrl;
  }

  @Override
  public GoCardlessClient createClientFor(UUID cleanAirZoneId) {
    return GoCardlessClient.newBuilder(getAccessTokenFor(cleanAirZoneId))
        .withBaseUrl(rootUrl)
        .build();
  }
}
