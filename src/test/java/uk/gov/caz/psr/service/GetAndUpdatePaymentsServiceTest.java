package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class GetAndUpdatePaymentsServiceTest {

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @InjectMocks
  private GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedNullValue() {
    // given
    UUID id = null;

    // when
    Throwable throwable = catchThrowable(() -> getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(id));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("ID cannot be null");
  }

  @Test
  public void shouldReturnEmptyOptionalIfPaymentIsNotFoundInDatabase() {
    // given
    UUID paymentId = UUID.fromString("a80d2cc8-f97a-11e9-9272-1b75c20437eb");
    given(internalPaymentsRepository.findById(paymentId))
        .willReturn(Optional.empty());

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(anyString());
    verify(internalPaymentsRepository, never()).update(any());
  }

  @Test
  public void shouldNotUpdatePaymentStatusIfExternalPaymentIdIsNull() {
    // given
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = null;
    mockInternalPaymentWith(paymentId, externalId);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(anyString());
    verify(internalPaymentsRepository, never()).update(any());
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId) {
    Payment payment =
        TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId);
    given(internalPaymentsRepository.findById(paymentId))
        .willReturn(Optional.of(payment));
    return payment;
  }
}
