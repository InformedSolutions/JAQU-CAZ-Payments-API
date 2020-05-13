package uk.gov.caz.psr.repository;

import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.caz.psr.dto.external.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.dto.external.directdebit.mandates.CreateMandateRequest;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * REST http client for GOV UK PAY service dealing with direct-debit-related endpoints.
 */
@Slf4j
@Repository
public class ExternalDirectDebitRepository {

  public static final String COLLECT_PAYMENT_URI = "/v1/directdebit/payments";
  public static final String CREATE_MANDATE_URI = "/v1/directdebit/mandates";
  public static final String GET_MANDATE_URI_TEMPLATE = "/v1/directdebit/mandates/{mandateId}";

  private final RestTemplate restTemplate;
  private final CredentialRetrievalManager credentialRetrievalManager;
  private final String rootUrl;

  /**
   * Constructor for {@link ExternalDirectDebitRepository}.
   */
  public ExternalDirectDebitRepository(
      @Value("${services.gov-uk-pay.root-url}") String rootUrl,
      RestTemplateBuilder restTemplateBuilder,
      CredentialRetrievalManager credentialRetrievalManager) {
    this.rootUrl = rootUrl;
    this.restTemplate = restTemplateBuilder.build();
    this.credentialRetrievalManager = credentialRetrievalManager;
  }

  /**
   * Creates a direct debit mandate for the CAZ account in the GOV UK Pay service.
   */
  public MandateResponse createMandate(String returnUrl, String reference, UUID cleanAirZoneId) {
    try {
      log.info("Create a new mandate: start");
      RequestEntity<CreateMandateRequest> request = RequestEntity
          .post(URI.create(rootUrl + CREATE_MANDATE_URI))
          .header("Authorization", "Bearer " + getDirectDebitApiKeyFor(cleanAirZoneId))
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .body(buildCreateMandateRequest(returnUrl, reference, cleanAirZoneId));
      ResponseEntity<MandateResponse> response = restTemplate
          .exchange(request, MandateResponse.class);
      return response.getBody();
    } catch (RestClientResponseException e) {
      log.error("Error while creating the direct debit payment: '{}', response body: '{}'",
          e.getMessage(), e.getResponseBodyAsString());
      throw new ExternalServiceCallException(e.getMessage());
    } catch (ResourceAccessException e) {
      log.error("I/O error while creating the direct debit payment: '{}'", e.getMessage());
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      log.info("Create a new mandate: finish");
    }
  }

  /**
   * Fetches a direct debit mandate for the CAZ account in the GOV UK Pay service by its
   * {@code mandateId} identifier.
   */
  public MandateResponse getMandate(String mandateId, UUID cleanAirZoneId) {
    try {
      log.info("Get mandate '{}': start", mandateId);
      ResponseEntity<MandateResponse> response = restTemplate.exchange(
          buildGetMandateRequest(mandateId, cleanAirZoneId),
          MandateResponse.class
      );
      return response.getBody();
    } catch (RestClientResponseException e) {
      log.error("Error while getting the mandate '{}': '{}', response body: '{}'", mandateId,
          e.getMessage(), e.getResponseBodyAsString());
      throw new ExternalServiceCallException(e.getMessage());
    } catch (ResourceAccessException e) {
      log.error("I/O error while getting the mandate '{}': '{}'", mandateId, e.getMessage());
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      log.info("Get mandate '{}': finish", mandateId);
    }
  }

  /**
   * Collects a payment using the provided mandate ({@code mandateId}).
   */
  public DirectDebitPayment collectPayment(String mandateId, int amount, String reference,
      UUID cleanAirZoneId) {
    try {
      log.info("Collect the direct debit payment: start");
      RequestEntity<CreateDirectDebitPaymentRequest> request = RequestEntity
          .post(URI.create(rootUrl + COLLECT_PAYMENT_URI))
          .header("Authorization", "Bearer " + getDirectDebitApiKeyFor(cleanAirZoneId))
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .body(buildCollectDirectDebitPaymentRequest(mandateId, amount, reference));
      ResponseEntity<DirectDebitPayment> directDebitPayment = restTemplate
          .exchange(request, DirectDebitPayment.class);
      return directDebitPayment.getBody();
    } catch (RestClientResponseException e) {
      log.error("Error while collecting the direct debit payment: '{}', response body: '{}'",
          e.getMessage(), e.getResponseBodyAsString());
      throw new ExternalServiceCallException(e.getMessage());
    } catch (ResourceAccessException e) {
      log.error("I/O error while collecting the direct debit payment: '{}'", e.getMessage());
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      log.info("Collect the direct debit payment: finish");
    }
  }

  /**
   * Creates a request with a payload for creating a mandate.
   */
  private CreateMandateRequest buildCreateMandateRequest(String returnUrl, String reference,
      UUID cleanAirZoneId) {
    return CreateMandateRequest.builder()
        .description("Mandate for CAZ " + cleanAirZoneId)
        .reference(reference)
        .returnUrl(returnUrl)
        .build();
  }

  /**
   * Creates a request for getting a mandate.
   */
  private RequestEntity<Void> buildGetMandateRequest(String mandateId, UUID cleanAirZoneId) {
    return RequestEntity.get(
            UriComponentsBuilder.fromUriString(rootUrl + GET_MANDATE_URI_TEMPLATE).build(mandateId))
            .header("Authorization", "Bearer " + getDirectDebitApiKeyFor(cleanAirZoneId))
            .accept(MediaType.APPLICATION_JSON)
            .build();
  }

  /**
   * Creates a request with a payload for collecting a payment.
   */
  private CreateDirectDebitPaymentRequest buildCollectDirectDebitPaymentRequest(String mandateId,
      int amount, String reference) {
    return CreateDirectDebitPaymentRequest.builder()
            .amount(amount)
            .description("Driving in a Clean Air Zone charge")
            .reference(reference)
            .mandateId(mandateId)
            .build();
  }

  /**
   * Returns Direct Debit API Key for the provided {@code cleanAirZoneId}.
   */
  private String getDirectDebitApiKeyFor(UUID cleanAirZoneId) {
    String apiKey = credentialRetrievalManager.getDirectDebitApiKey(cleanAirZoneId)
        .orElseThrow(() -> new IllegalStateException("Direct Debit API key has not been set for "
            + "Clean Air Zone " + cleanAirZoneId));
    logMaskedApiKey(apiKey, cleanAirZoneId);
    return apiKey;
  }

  /**
   * Logs first 3 characters of the api key.
   */
  private void logMaskedApiKey(String apiKey, UUID cleanAirZoneId) {
    log.info("Direct Debit GOV UK PAY api key for CAZ '{}': {}", cleanAirZoneId,
        uk.gov.caz.psr.util.Strings.mask(apiKey));
  }
}
