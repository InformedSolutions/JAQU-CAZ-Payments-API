package uk.gov.caz.psr.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.caz.psr.dto.external.CreateCardPaymentRequest;
import uk.gov.caz.psr.dto.external.CreatePaymentResult;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

/**
 * REST http client for GOV UK PAY service.
 */
@Slf4j
@Repository
public class ExternalCardPaymentsRepository {

  @VisibleForTesting
  public static final String FIND_BY_ID_URI_TEMPLATE = "/v1/payments/{paymentId}";
  @VisibleForTesting
  public static final String CREATE_URI = "/v1/payments";

  private final RestTemplate restTemplate;
  private final CredentialRetrievalManager credentialRetrievalManager;
  private final String rootUrl;

  /**
   * Constructor for {@link ExternalCardPaymentsRepository}.
   */
  public ExternalCardPaymentsRepository(@Value("${services.gov-uk-pay.root-url}") String rootUrl,
      RestTemplateBuilder restTemplateBuilder,
      CredentialRetrievalManager credentialRetrievalManager) {
    this.rootUrl = rootUrl;
    this.restTemplate = restTemplateBuilder.build();
    this.credentialRetrievalManager = credentialRetrievalManager;
  }

  /**
   * Filters through a {@link Payment}'s {@link uk.gov.caz.psr.model.EntrantPayment}s to retrieve
   * a Clean Air Zone ID.
   *
   * @param payment an instance of a {@link Payment} object
   * @return the API key
   */
  private String getApiKeyFor(Payment payment) {
    UUID cleanAirZoneId = payment.getCleanAirZoneId();
    return getApiKeyFor(cleanAirZoneId);
  }

  /**
   * Retrieve the API key from the external credentials repository.
   *
   * @param cleanAirZoneId a {@link UUID} for the Clean Air Zone.
   * @return the API key
   */
  private String getApiKeyFor(UUID cleanAirZoneId) {
    String apiKey = credentialRetrievalManager.getCardApiKey(cleanAirZoneId)
        .orElseThrow(() -> new IllegalStateException(
            "The API key has not been set for Clean Air Zone " + cleanAirZoneId));
    logMaskedApiKey(apiKey, cleanAirZoneId);
    return apiKey;
  }

  /**
   * Logs first 3 characters of the api key.
   */
  private void logMaskedApiKey(String apiKey, UUID cleanAirZoneId) {
    log.info("GOV UK PAY api key for CAZ '{}': {}", cleanAirZoneId,
        uk.gov.caz.psr.util.Strings.mask(apiKey));
  }

  /**
   * Creates a payment in the external service for the provided {@code payment} entity.
   *
   * @param payment Object of the internal payment.
   * @param returnUrl Url from the Fronted to redirect after payment in GOV.UK PAY.
   * @return An instance of {@link Payment} with {@code externalPaymentId} and {@code nextUrl}
   *     properties set.
   * @throws NullPointerException if {@code payment} or {@link Payment#getId()} is null
   * @throws IllegalArgumentException if {@code returnUrl} is null or empty
   */
  public Payment create(Payment payment, String returnUrl) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(payment.getId(), "Payment must have set its internal identifier");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(returnUrl),
        "Return url cannot be null or empty");
    try {
      log.info("Create the payment for {}: start", payment.getId());
      RequestEntity<CreateCardPaymentRequest> request =
          buildRequestEntityForCreate(getApiKeyFor(payment), buildCreateBody(payment, returnUrl));
      ResponseEntity<CreatePaymentResult> responseEntity =
          restTemplate.exchange(request, CreatePaymentResult.class);
      CreatePaymentResult responseBody = responseEntity.getBody();
      ExternalPaymentStatus externalPaymentStatus =
          toModelStatus(responseBody.getState().getStatus());
      return payment.toBuilder()
          .externalId(responseBody.getPaymentId())
          .submittedTimestamp(LocalDateTime.now())
          .externalPaymentStatus(externalPaymentStatus)
          .nextUrl(responseBody.getLinks().getNextUrl().getHref())
          .build();
    } catch (RestClientException e) {
      log.error("Error while creating the payment for '{}': {}", payment.getId(), e.getMessage());
      throw e;
    } finally {
      log.info("Create the payment for {}: finish", payment.getId());
    }
  }

  /**
   * Converts a status returned from the GOV UK Pay service to {@link ExternalPaymentStatus}. If the
   * value does not match any existing one, {@link IllegalArgumentException} is rethrown.
   */
  private ExternalPaymentStatus toModelStatus(String status) {
    try {
      return ExternalPaymentStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Unrecognized external payment status '{}'", status);
      throw e;
    }
  }

  /**
   * Gets payment details by its identifier.
   *
   * @param id ID of the payment.
   * @param cleanAirZoneId a {@link UUID} identifying the Clean Air Zone the payment was made
   *     in.
   * @return {@link GetPaymentResult} wrapped in {@link Optional} if the payment exist, {@link
   *     Optional#empty()} otherwise.
   * @throws IllegalArgumentException if {@code id} is null or empty
   */
  public Optional<GetPaymentResult> findByIdAndCazId(String id, UUID cleanAirZoneId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "ID cannot be null or empty");
    try {
      log.info("Get payment by id '{}' : start", id);
      RequestEntity<Void> request = buildRequestEntityForFindById(getApiKeyFor(cleanAirZoneId), id);
      ResponseEntity<GetPaymentResult> responseEntity =
          restTemplate.exchange(request, GetPaymentResult.class);
      return Optional.of(responseEntity.getBody());
    } catch (NotFound e) {
      log.error("Payment with id '{}' not found", id);
      return Optional.empty();
    } catch (RestClientException e) {
      log.error("Error while getting the payment by id '{}'", id);
      throw e;
    } finally {
      log.info("Get payment by id '{}' : finish", id);
    }
  }

  /**
   * Creates a request entity for {@code findBy} operation.
   */
  private RequestEntity<Void> buildRequestEntityForFindById(String apiKey, String id) {
    return RequestEntity.get(buildFindByUri(id)).header("Authorization", "Bearer " + apiKey)
        .accept(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Creates {@link URI} for {@code findBy} operation.
   */
  private URI buildFindByUri(String id) {
    return UriComponentsBuilder.fromUriString(rootUrl + FIND_BY_ID_URI_TEMPLATE).build(id);
  }

  /**
   * Creates a request entity for {@code create} operation.
   */
  private RequestEntity<CreateCardPaymentRequest> buildRequestEntityForCreate(String apiKey,
      CreateCardPaymentRequest body) {
    return RequestEntity.post(URI.create(rootUrl + CREATE_URI))
        .header("Authorization", "Bearer " + apiKey).accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON).body(body);
  }

  /**
   * Creates {@link URI} for {@code create} operation.
   */
  private CreateCardPaymentRequest buildCreateBody(Payment payment, String returnUrl) {
    return CreateCardPaymentRequest.builder()
        .amount(payment.getTotalPaid())
        .description("Driving in a Clean Air Zone charge")
        .reference(payment.getReferenceNumber().toString())
        .moto(payment.isTelephonePayment())
        .returnUrl(returnUrl).build();
  }

}
