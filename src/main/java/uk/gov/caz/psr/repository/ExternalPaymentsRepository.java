package uk.gov.caz.psr.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
import uk.gov.caz.psr.dto.external.CreatePaymentResult;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.Payment;

/**
 * REST http client for GOV UK PAY service.
 */
@Slf4j
@Repository
public class ExternalPaymentsRepository {

  private static final String FIND_BY_ID_URI_TEMPLATE = "/v1/payments/{paymentId}";
  private static final String CREATE_URI = "/v1/payments";

  private final RestTemplate restTemplate;
  private final String rootUrl;

  /**
   * Constructor for {@link ExternalPaymentsRepository}.
   */
  public ExternalPaymentsRepository(@Value("${services.gov-uk-pay.root-url}") String rootUrl,
      @Value("${services.gov-uk-pay.api-key}") String apiKey,
      RestTemplateBuilder restTemplateBuilder) {
    this.rootUrl = rootUrl;
    this.restTemplate = restTemplateBuilder.interceptors(apiKeyHeaderInjector(apiKey)).build();
  }

  /**
   * Create Payment for provided payment.
   *
   * @param payment   Object of the internal payment.
   * @param returnUrl Url from the Fronted to redirect after payment in GOV.UK PAY.
   * @return {@link GetPaymentResult} wrapped in {@link Optional} if the payment exist, {@link
   * Optional#empty()} otherwise.
   */
  public Optional<Payment> create(Payment payment, String returnUrl) {
    try {
      log.info("Create the payment for {}: start", payment.getId());
      RequestEntity<Map<String, Object>> request = buildRequestEntityForCreate(
          buildCreateBody(payment, returnUrl));
      restTemplate.exchange(request, CreatePaymentResult.class);
      return Optional.of(payment);
    } catch (RestClientException e) {
      log.error("Error while creating the payment for '{}'", payment.getId(), e);
      return Optional.empty();
    } finally {
      log.info("Create the payment for {}: finish", payment.getId());
    }
  }

  /**
   * Gets payment details by its identifier.
   *
   * @param id ID of the payment.
   * @return {@link GetPaymentResult} wrapped in {@link Optional} if the payment exist, {@link
   * Optional#empty()} otherwise.
   */
  public Optional<Payment> findById(String id) {
    try {
      log.info("Get payment by id '{}' : start", id);
      RequestEntity<Void> request = buildRequestEntityForFindById(id);
      ResponseEntity<GetPaymentResult> responseEntity = restTemplate.exchange(request,
          GetPaymentResult.class);
      return Optional.of(responseEntity.getBody().toPayment());
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
  private RequestEntity<Map<String, Object>> buildRequestEntityForCreate(Map<String, Object> body) {
    return RequestEntity.post(buildCreateUri())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body);
  }

  /**
   * Creates {@link URI} for {@code create} operation.
   */
  private URI buildCreateUri() {
    return URI.create(rootUrl + CREATE_URI);
  }

  /**
   * Creates {@link URI} for {@code create} operation.
   */
  private Map<String, Object> buildCreateBody(Payment payment, String returnUrl) {
    Map<String, Object> root = new HashMap<>();
    root.put("amount", payment.getChargePaid());
    root.put("reference", payment.getId());
    root.put("description", "Payment for #" + payment.getId());
    root.put("return_url", returnUrl);
    return root;
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
