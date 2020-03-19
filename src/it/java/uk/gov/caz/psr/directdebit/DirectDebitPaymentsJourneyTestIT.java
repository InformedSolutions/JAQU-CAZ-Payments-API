package uk.gov.caz.psr.directdebit;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;
import uk.gov.caz.psr.util.SecretsManagerInitialisation;

@FullyRunningServerIntegrationTest
public class DirectDebitPaymentsJourneyTestIT {

  @Autowired
  private ExternalDirectDebitRepository externalDirectDebitRepository;

  @Autowired
  private SecretsManagerInitialisation secretsManagerInitialisation;
  @Value("${aws.direct-debit-secret-name}")
  private String apiKeySecretName;

  private ClientAndServer govUkPayMockServer;

  @BeforeEach
  public void startMockGovUkPayServer() {
    govUkPayMockServer = startClientAndServer(1080);
  }

  @BeforeEach
  public void setApiKeyInSecretsManager() {
    secretsManagerInitialisation.createSecret(apiKeySecretName);
  }

  @AfterEach
  public void stopMockGovUkPayServer() {
    govUkPayMockServer.stop();
  }

  @Test
  public void testIntegrationWithExternalService() {
    mockMandateCreationResponse();
    mockMandateQueryResponse();
    mockCollectPaymentResponse();

    UUID cleanAirZoneId = UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
    MandateResponse mandate = createMandate(cleanAirZoneId);
    MandateResponse obtainedMandate = getMandate(cleanAirZoneId, mandate);
    DirectDebitPayment payment = collectPaymentAgainstMandate(cleanAirZoneId, obtainedMandate);
  }

  private DirectDebitPayment collectPaymentAgainstMandate(UUID cleanAirZoneId,
      MandateResponse obtainedMandate) {
    return externalDirectDebitRepository.collectPayment(
        obtainedMandate.getMandateId(),
        100,
        "12345",
        cleanAirZoneId
    );
  }

  private MandateResponse getMandate(UUID cleanAirZoneId, MandateResponse mandate) {
    return externalDirectDebitRepository
        .getMandate(mandate.getMandateId(), cleanAirZoneId);
  }

  private MandateResponse createMandate(UUID cleanAirZoneId) {
    return externalDirectDebitRepository.createMandate(
        "https://informed.com/transactions/12345",
        "12345",
        cleanAirZoneId
    );
  }

  private void mockMandateCreationResponse() {
    govUkPayMockServer
        .when(HttpRequest.request().withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withPath(ExternalDirectDebitRepository.CREATE_MANDATE_URI))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.CREATED.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("create-mandate-response.json")));
  }

  private void mockMandateQueryResponse() {
    govUkPayMockServer
        .when(HttpRequest.request().withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/directdebit/mandates/.*"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("get-mandate-response.json")));
  }

  private void mockCollectPaymentResponse() {
    govUkPayMockServer
        .when(HttpRequest.request().withMethod("POST")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withPath(ExternalDirectDebitRepository.COLLECT_PAYMENT_URI))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile("collect-payment-response.json")));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/directdebit/" + filename), Charsets.UTF_8);
  }
}
