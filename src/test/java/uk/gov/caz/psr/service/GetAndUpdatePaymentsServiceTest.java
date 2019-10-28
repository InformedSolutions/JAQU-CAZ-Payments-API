package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class GetAndUpdatePaymentsServiceTest {

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @InjectMocks
  private GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @Test
  public void shouldReturnEmptyOptionalIfPaymentIsNotFoundInDatabase() {
    // given
    UUID paymentId = UUID.fromString("a80d2cc8-f97a-11e9-9272-1b75c20437eb");
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.empty());

    // when
    Optional<Payment> result = getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

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
    Optional<Payment> result = getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(anyString());
    verify(internalPaymentsRepository, never()).update(any());
  }

  @Test
  public void shouldNotUpdatePaymentStatusIfExternalPaymentIsNotFound() {
    // given
    UUID paymentId = UUID.fromString("24e004e7-b9eb-42f3-89fc-53369955407c");
    String externalId = "external-id";
    mockInternalPaymentWith(paymentId, externalId);
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.empty());

    // when
    Optional<Payment> result = getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(internalPaymentsRepository, never()).update(any());
  }

  @Test
  public void shouldUpdatePaymentStatusIfExternalPaymentIsFound() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    PaymentStatus externalStatus = PaymentStatus.FAILED;
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockExternalPaymentWithStatus(externalId, payment, externalStatus);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    Payment internalPaymentWithExternalStatus = payment.toBuilder().status(externalStatus).build();
    assertThat(result).contains(internalPaymentWithExternalStatus);
    verify(internalPaymentsRepository).update(internalPaymentWithExternalStatus);
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId) {
    Payment payment = createPayment(paymentId, externalId);
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.of(payment));
    return payment;
  }

  private Payment createPayment(UUID paymentId, String externalId) {
    return Payment.builder().id(paymentId).externalPaymentId(externalId)
        .status(PaymentStatus.STARTED).build();
  }

  private Payment mockExternalPaymentWithStatus(String externalId, Payment payment,
      PaymentStatus status) {
    Payment externalPayment = toExternalPaymentWithStatus(payment, status);
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.of(
        externalPayment));
    return externalPayment;
  }

  private Payment toExternalPaymentWithStatus(Payment payment, PaymentStatus status) {
    return payment.toBuilder()
        .id(null)
        .status(status)
        .build();
  }
}