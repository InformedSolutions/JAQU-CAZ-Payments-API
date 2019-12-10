package uk.gov.caz.psr.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
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

/**
 * REST http client for GOV UK PAY service.
 */
@Slf4j
@Repository
public class ExternalPaymentsRepository {

  @VisibleForTesting
  public static final String FIND_BY_ID_URI_TEMPLATE = "/v1/payments/{paymentId}";
  @VisibleForTesting
  public static final String CREATE_URI = "/v1/payments";

  private final RestTemplate restTemplate;
  private final String rootUrl;
  private final URI createPaymentUri;

  /**
   * Constructor for {@link ExternalPaymentsRepository}.
   */
  public ExternalPaymentsRepository(@Value("${services.gov-uk-pay.root-url}") String rootUrl,
      @Value("${services.gov-uk-pay.api-key}") String apiKey,
      RestTemplateBuilder restTemplateBuilder) {
    this.rootUrl = rootUrl;
    this.restTemplate = restTemplateBuilder.interceptors(apiKeyHeaderInjector(apiKey)).build();
    this.createPaymentUri = URI.create(rootUrl + CREATE_URI);
    logMaskedApiKey(apiKey);
  }

  /**
   * Logs first 3 characters of the api key.
   */
  private void logMaskedApiKey(String apiKey) {
    int toReveal = Math.min(3, apiKey.length());
    int toMask = apiKey.length() - toReveal;
    log.info("GOV UK PAY api key: {}{}", apiKey.substring(0, toReveal), Strings.repeat("*",
        toMask));
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
    Preconditions.checkNotNull(payment.getId(),
        "Payment must have set its internal identifier");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(returnUrl),
        "Return url cannot be null or empty");
    try {
      log.info("Create the payment for {}: start", payment.getId());
      RequestEntity<CreateCardPaymentRequest> request = buildRequestEntityForCreate(
          buildCreateBody(payment, returnUrl));
      ResponseEntity<CreatePaymentResult> responseEntity = restTemplate.exchange(request,
          CreatePaymentResult.class);
      CreatePaymentResult responseBody = responseEntity.getBody();
      ExternalPaymentStatus externalPaymentStatus = toModelStatus(
          responseBody.getState().getStatus());
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
   * Converts a status returned from the GOV UK Pay service to {@link
   * ExternalPaymentStatus}. If the value does not match any existing one, {@link
   * ExternalPaymentStatus#UNKNOWN} is returned.
   */
  private ExternalPaymentStatus toModelStatus(String status) {
    try {
      return ExternalPaymentStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Unrecognized external payment status '{}', returning {}", status,
          ExternalPaymentStatus.UNKNOWN);
      return ExternalPaymentStatus.UNKNOWN;
    }
  }

  /**
   * Gets payment details by its identifier.
   *
   * @param id ID of the payment.
   * @return {@link GetPaymentResult} wrapped in {@link Optional} if the payment exist, {@link
   *     Optional#empty()} otherwise.
   * @throws IllegalArgumentException if {@code id} is null or empty
   */
  public Optional<GetPaymentResult> findById(String id) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "ID cannot be null or empty");
    try {
      log.info("Get payment by id '{}' : start", id);
      RequestEntity<Void> request = buildRequestEntityForFindById(id);
      ResponseEntity<GetPaymentResult> responseEntity = restTemplate.exchange(request,
          GetPaymentResult.class);
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
  private RequestEntity<Void> buildRequestEntityForFindById(String id) {
    return RequestEntity.get(buildFindByUri(id))
        .accept(MediaType.APPLICATION_JSON)
        .build();
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
  private RequestEntity<CreateCardPaymentRequest> buildRequestEntityForCreate(
      CreateCardPaymentRequest body) {
    return RequestEntity.post(createPaymentUri)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  /**
   * Creates {@link URI} for {@code create} operation.
   */
  private CreateCardPaymentRequest buildCreateBody(Payment payment, String returnUrl) {
    return CreateCardPaymentRequest.builder()
        .amount(payment.getTotalPaid())
        .description("Payment for #" + payment.getId())
        .reference(payment.getId().toString())
        .returnUrl(returnUrl)
        .build();
  }

  /**
   * Creates {@link ClientHttpRequestInterceptor} which injects authorization header in an automatic
   * fashion for every request.
   */
  private ClientHttpRequestInterceptor apiKeyHeaderInjector(String apiKey) {
    return (request, payload, execution) -> {
      HttpHeaders headers = request.getHeaders();
      headers.setBearerAuth(apiKey);
      return execution.execute(request, payload);
    };
  }
}
