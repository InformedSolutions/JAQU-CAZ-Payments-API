package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gocardless.GoCardlessClient;
import com.gocardless.errors.InvalidStateException;
import com.gocardless.resources.Payment;
import com.gocardless.resources.Payment.Status;
import com.gocardless.services.PaymentService;
import com.gocardless.services.PaymentService.PaymentCreateRequest;
import com.gocardless.services.PaymentService.PaymentCreateRequest.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.service.exception.CollectDirectDebitPaymentException;

@ExtendWith(MockitoExtension.class)
class DirectDebitPaymentServiceTest {

  @Mock
  private AbstractGoCardlessClientFactory goCardlessClientFactory;

  @Mock
  private GoCardlessClient goCardlessClient;

  @InjectMocks
  DirectDebitPaymentService directDebitPaymentService;

  private final static UUID ANY_PAYMENT_ID = UUID.randomUUID();
  private final static String ANY_EXTERNAL_PAYMENT_ID = "ExTeRnAl_PaYmEnT_iD";
  private final static UUID ANY_CLEAN_AIR_ZONE_ID = UUID.randomUUID();
  private final static int ANY_AMOUNT = 1234;
  private final static Long ANY_REFERENCE = 100001L;
  private final static String ANY_MANDATE_ID = "MaNdATeID";
  private final static Status ANY_PAYMENT_STATUS = Status.PENDING_SUBMISSION;

  @Nested
  class CollectPayment {

    @Test
    public void shouldReturnDirectDebitPaymentOnSuccessfulCreation() {
      // given
      mockSuccessfulPaymentCreation();

      // when
      DirectDebitPayment directDebitPayment = directDebitPaymentService
          .collectPayment(ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, ANY_AMOUNT, ANY_REFERENCE,
              ANY_MANDATE_ID);

      // then
      assertThat(directDebitPayment.getPaymentId()).isEqualTo(ANY_EXTERNAL_PAYMENT_ID);
    }

    @Test
    public void shouldThrowCollectDirectDebitPaymentExceptionOnUnsuccessfulCreation() {
      // given
      mockUnsuccessfulPaymentCreation();

      // when
      Throwable throwable = catchThrowable(() -> directDebitPaymentService
          .collectPayment(ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, ANY_AMOUNT, ANY_REFERENCE,
              ANY_MANDATE_ID));

      // then
      assertThat(throwable).isInstanceOf(CollectDirectDebitPaymentException.class);
    }

    private void mockUnsuccessfulPaymentCreation() {
      PaymentCreateRequest createRequest = mockPaymentCreateRequest();
      InvalidStateException exception = Mockito.mock(InvalidStateException.class);
      when(exception.getMessage()).thenReturn("Mandate is not valid");
      when(createRequest.execute()).thenThrow(exception);
    }

    private void mockSuccessfulPaymentCreation() {
      PaymentCreateRequest createRequest = mockPaymentCreateRequest();

      Payment response = Mockito.mock(Payment.class);

      when(createRequest.execute()).thenReturn(response);
      when(response.getId()).thenReturn(ANY_EXTERNAL_PAYMENT_ID);
      when(response.getStatus()).thenReturn(ANY_PAYMENT_STATUS);
    }

    private PaymentCreateRequest mockPaymentCreateRequest() {
      when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
          .thenReturn(goCardlessClient);
      PaymentService paymentService = Mockito.mock(PaymentService.class);
      PaymentCreateRequest createRequest = Mockito.mock(PaymentCreateRequest.class);

      when(goCardlessClient.payments()).thenReturn(paymentService);
      when(paymentService.create()).thenReturn(createRequest);
      when(createRequest.withAmount(anyInt())).thenReturn(createRequest);
      when(createRequest.withCurrency(any(Currency.class))).thenReturn(createRequest);
      when(createRequest.withReference(anyString())).thenReturn(createRequest);
      when(createRequest.withLinksMandate(anyString())).thenReturn(createRequest);
      when(createRequest.withIdempotencyKey(anyString())).thenReturn(createRequest);

      return createRequest;
    }
  }
}