package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.service.authentication.CredentialRetrievalManager;

@ExtendWith(MockitoExtension.class)
class ExternalDirectDebitRepositoryTest {

  private static final UUID CAZ_ID = UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");

  @Mock
  private CredentialRetrievalManager credentialRetrievalManager;

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  private ExternalDirectDebitRepository repository;

  @BeforeEach
  public void initRepo() {
    mockRestTemplate();
    repository = new ExternalDirectDebitRepository("some-url", restTemplateBuilder, credentialRetrievalManager);
  }

  private void mockRestTemplate() {
    given(restTemplateBuilder.build()).willReturn(restTemplate);
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenApiKeyIsNotFound() {
    mockApiKeyAbsence();

    Throwable throwable = catchThrowable(() -> repository.getMandate("mandateId", CAZ_ID));

    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Direct Debit API key has not been set");
  }

  @Nested
  class CreateMandate {

    @Test
    public void shouldRethrowRestClientResponseException() {
      mockApiKeyPresence();
      mockRestClientResponseExceptionUponRestTemplateCallForResponseClass(MandateResponse.class);

      Throwable throwable = catchThrowable(() -> repository.createMandate("return-url", "reference", CAZ_ID));

      assertThat(throwable).isInstanceOf(RestClientResponseException.class);
    }

    @Test
    public void shouldRethrowResourceAccessException() {
      mockApiKeyPresence();
      mockResourceAccessExceptionUponRestTemplateCallForResponseClass(MandateResponse.class);

      Throwable throwable = catchThrowable(() -> repository.createMandate("return-url", "reference", CAZ_ID));

      assertThat(throwable).isInstanceOf(ResourceAccessException.class);
    }
  }

  @Nested
  class GetMandate {

    @Test
    public void shouldRethrowRestClientResponseException() {
      mockApiKeyPresence();
      mockRestClientResponseExceptionUponRestTemplateCallForResponseClass(MandateResponse.class);

      Throwable throwable = catchThrowable(() -> repository.getMandate("mandate-id", CAZ_ID));

      assertThat(throwable).isInstanceOf(RestClientResponseException.class);
    }

    @Test
    public void shouldRethrowResourceAccessException() {
      mockApiKeyPresence();
      mockResourceAccessExceptionUponRestTemplateCallForResponseClass(MandateResponse.class);

      Throwable throwable = catchThrowable(() -> repository.getMandate("mandate-id", CAZ_ID));

      assertThat(throwable).isInstanceOf(ResourceAccessException.class);
    }
  }

  @Nested
  class CollectPayments {

    @Test
    public void shouldRethrowRestClientResponseException() {
      mockApiKeyPresence();
      mockRestClientResponseExceptionUponRestTemplateCallForResponseClass(DirectDebitPayment.class);

      Throwable throwable = catchThrowable(() -> repository.collectPayment("mandate-id", 200, "ref", CAZ_ID));

      assertThat(throwable).isInstanceOf(RestClientResponseException.class);
    }

    @Test
    public void shouldRethrowResourceAccessException() {
      mockApiKeyPresence();
      mockResourceAccessExceptionUponRestTemplateCallForResponseClass(DirectDebitPayment.class);

      Throwable throwable = catchThrowable(() -> repository.collectPayment("mandate-id", 200, "ref", CAZ_ID));

      assertThat(throwable).isInstanceOf(ResourceAccessException.class);
    }
  }

  private void mockApiKeyAbsence() {
    given(credentialRetrievalManager.getDirectDebitApiKey(any()))
        .willReturn(Optional.empty());
  }

  private void mockApiKeyPresence() {
    given(credentialRetrievalManager.getDirectDebitApiKey(any()))
        .willReturn(Optional.of("my-api-key"));
  }

  private <T> void mockRestClientResponseExceptionUponRestTemplateCallForResponseClass(
      Class<T> responseClass) {
    given(restTemplate.exchange(any(), Mockito.eq(responseClass))).willThrow(
        new RestClientResponseException("msg", 400, "status", null, null, null));
  }

  private <T> void mockResourceAccessExceptionUponRestTemplateCallForResponseClass(
      Class<T> responseClass) {
    given(restTemplate.exchange(any(), Mockito.eq(responseClass))).willThrow(
        new ResourceAccessException("i/o exception"));
  }
}