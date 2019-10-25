package uk.gov.caz.psr.repository;

import java.net.URI;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.caz.psr.dto.external.GetPaymentResult;

/**
 * REST http client for GOV UK PAY service.
 */
@Slf4j
@Repository
public class ExternalPaymentsRepository {

  private static final String FIND_BY_ID_URI = "/v1/payments/{paymentId}";

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
   * Gets payment details by its identifier.
   *
   * @param id ID of the payment.
   * @return {@link GetPaymentResult} wrapped in {@link Optional} if the payment exist, {@link
   *     Optional#empty()} otherwise.
   */
  public Optional<GetPaymentResult> findById(String id) {
    try {
      log.info("Get payment by id '{}' : start", id);
      RequestEntity<Void> request = buildRequestEntityForFindById(id);
      ResponseEntity<GetPaymentResult> responseEntity = restTemplate.exchange(request,
          GetPaymentResult.class);
      GetPaymentResult body = responseEntity.getBody();
      return Optional.of(body);
    } catch (RestClientException e) {
      log.error("Error while getting the payment by id '{}'", id, e);
      return Optional.empty();
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
    return UriComponentsBuilder.fromUriString(rootUrl + FIND_BY_ID_URI).build(id);
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
