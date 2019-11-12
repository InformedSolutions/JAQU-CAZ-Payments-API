package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.psr.dto.external.CreatePaymentResult;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.Link;
import uk.gov.caz.psr.dto.external.PaymentLinks;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentsRepositoryTest {

  private static final String ANY_ROOT_URL = "http://localhost";
  private static final String ANY_API_KEY = "test-api-key";
  private static final String ANY_RETURN_URL = "http://localhost/return-url";

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  private ExternalPaymentsRepository paymentsRepository;

  @BeforeEach
  public void setUp() {
    when(restTemplateBuilder
        .interceptors(any(ClientHttpRequestInterceptor.class)))
            .thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);

    paymentsRepository =
        new ExternalPaymentsRepository(ANY_ROOT_URL, restTemplateBuilder);
    paymentsRepository.setApiKey(ANY_API_KEY);
  }

  @Nested
  class Create {

    @Test
    public void shouldThrowNullPointerExceptionWhenPassedPaymentIsNull() {
      // given
      Payment payment = null;

      // when
      Throwable throwable = catchThrowable(
          () -> paymentsRepository.create(payment, ANY_RETURN_URL));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasNoId() {
      // given
      Payment payment = createPayment(null);

      // when
      Throwable throwable = catchThrowable(
          () -> paymentsRepository.create(payment, ANY_RETURN_URL));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment must have set its internal identifier");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenReturnUrlIsNull() {
      // given
      Payment payment = createPayment(
          UUID.fromString("26ca3b2a-fba9-11e9-9334-1fbaf36c3aee"));

      // when
      Throwable throwable =
          catchThrowable(() -> paymentsRepository.create(payment, null));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Return url cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenReturnUrlIsEmpty() {
      // given
      Payment payment = createPayment(
          UUID.fromString("5b793d4e-fba9-11e9-9334-6b0964eb9a87"));

      // when
      Throwable throwable =
          catchThrowable(() -> paymentsRepository.create(payment, ""));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Return url cannot be null or empty");
    }

    @Test
    public void shouldSetUnknownStatusIfNoneIsMatched() {
      // given
      UUID paymentId = UUID.fromString("9d4fc418-fbae-11e9-8f23-cf92e47420e6");
      mockRestTemplateResultWithUnrecognizedStatus();
      Payment payment = createPayment(paymentId);

      // when
      Payment result = paymentsRepository.create(payment, ANY_RETURN_URL);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    private void mockRestTemplateResultWithUnrecognizedStatus() {
      given(restTemplate.exchange(any(), eq(CreatePaymentResult.class)))
          .willReturn(ResponseEntity.ok(CreatePaymentResult.builder()
              .paymentId("ex-pay-id").amount(100)
              .links(PaymentLinks.builder()
                  .nextUrl(new Link("http://some-address.com", "GET")).build())
              .state(PaymentState.builder().status("not-recognized").build())
              .build()));
    }


    @Test
    public void shouldRethrowExceptionWhenCallFails() {
      // given
      Payment payment = createPayment(
          UUID.fromString("ef1aad78-fba7-11e9-9334-4b9678d9f25f"));
      given(restTemplate.exchange(any(), eq(CreatePaymentResult.class)))
          .willThrow(
              HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                  "", new HttpHeaders(), null, null));

      // when
      Throwable throwable = catchThrowable(
          () -> paymentsRepository.create(payment, ANY_RETURN_URL));

      // then
      assertThat(throwable).isInstanceOf(RestClientException.class);
    }

    private Payment createPayment(UUID paymentId) {
      return Payment.builder().id(paymentId).chargePaid(100)
          .paymentMethod(PaymentMethod.CREDIT_CARD)
          .status(PaymentStatus.STARTED).build();
    }
  }

  @Nested
  class FindById {

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenIdIsNull() {
      // given
      String id = null;

      // when
      Throwable throwable =
          catchThrowable(() -> paymentsRepository.findById(id));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenIdIsEmpty() {
      // given
      String id = "";

      // when
      Throwable throwable =
          catchThrowable(() -> paymentsRepository.findById(id));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID cannot be null or empty");
    }

    @Test
    public void shouldReturnEmptyOptionalWhen404StatusCodeIsReturned() {
      // given
      given(restTemplate.exchange(any(), eq(GetPaymentResult.class)))
          .willThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "",
              new HttpHeaders(), null, null));
      String id = "payment id";

      // when
      Optional<Payment> result = paymentsRepository.findById(id);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    public void shouldRethrowExceptionWhenCallFails() {
      // given
      given(restTemplate.exchange(any(), eq(GetPaymentResult.class))).willThrow(
          HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "",
              new HttpHeaders(), null, null));
      String id = "payment id";

      // when
      Throwable throwable =
          catchThrowable(() -> paymentsRepository.findById(id));

      // then
      assertThat(throwable).isInstanceOf(RestClientException.class);
    }

    @Test
    public void shouldCallRestTemplateExchange() {
      // given
      mockRestTemplateResult();
      String id = "payment id";

      // when
      Optional<Payment> result = paymentsRepository.findById(id);

      // then
      assertThat(result).isNotEmpty();
      verify(restTemplate).exchange(any(), eq(GetPaymentResult.class));
    }

    private void mockRestTemplateResult() {
      given(restTemplate.exchange(any(), eq(GetPaymentResult.class)))
          .willReturn(new ResponseEntity<>(GetPaymentResult.builder()
              .state(PaymentState.builder().status("success").build()).build(),
              HttpStatus.OK));
    }
  }
}
