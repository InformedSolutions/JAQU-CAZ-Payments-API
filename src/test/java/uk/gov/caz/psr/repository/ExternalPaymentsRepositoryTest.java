package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.psr.dto.external.GetPaymentResult;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentsRepositoryTest {

  private static final String ANY_ROOT_URL = "http://localhost";
  private static final String ANY_API_KEY = "apikey";

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  private ExternalPaymentsRepository paymentsRepository;

  @BeforeEach
  public void setUp() {
    when(restTemplateBuilder.interceptors(any(ClientHttpRequestInterceptor.class)))
        .thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);

    paymentsRepository = new ExternalPaymentsRepository(ANY_ROOT_URL, ANY_API_KEY,
        restTemplateBuilder);
  }

  @Test
  public void shouldReturnEmptyOptionalUponCallFailure() {
    // given
    given(restTemplate.exchange(any(), eq(GetPaymentResult.class))).willThrow(
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", new HttpHeaders(), null, null));
    String id = "payment id";

    // when
    Optional<GetPaymentResult> result = paymentsRepository.findById(id);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void shouldCallRestTemplateExchange() {
    // given
    given(restTemplate.exchange(any(), eq(GetPaymentResult.class)))
        .willReturn(new ResponseEntity<>(GetPaymentResult.builder().build(), HttpStatus.OK));
    String id = "payment id";

    // when
    Optional<GetPaymentResult> result = paymentsRepository.findById(id);

    // then
    assertThat(result).isNotEmpty();
    verify(restTemplate).exchange(any(), eq(GetPaymentResult.class));
  }
}