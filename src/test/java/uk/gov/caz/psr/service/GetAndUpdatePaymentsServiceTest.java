package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class GetAndUpdatePaymentsServiceTest {
  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentStatusUpdater paymentStatusUpdater;

  @InjectMocks
  private GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedNullValue() {
    // given
    UUID id = null;

    // when
    Throwable throwable = catchThrowable(() ->
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(id));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("ID cannot be null");
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenPaymentIsNotFoundExternally() {
    // given
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = "ext-id";
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockPaymentAbsenceInExternalService(payment);

    // when
    Throwable throwable = catchThrowable(() ->
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("External payment not found with id");
  }

  @Test
  public void shouldReturnEmptyOptionalIfPaymentIsNotFoundInDatabase() {
    // given
    UUID paymentId = UUID.fromString("a80d2cc8-f97a-11e9-9272-1b75c20437eb");
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.empty());

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(any());
    verify(paymentStatusUpdater, never()).updateWithStatus(any(), any(), any());
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
    verify(externalPaymentsRepository, never()).findById(any());
    verify(paymentStatusUpdater, never()).updateWithStatus(any(), any(), any());
  }

  @Test
  public void shouldNotUpdatePaymentStatusIfExternalPaymentStatusNotChanged() {
    // given
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = "ext-id";
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockSameExternalStatusFor(payment);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).contains(payment);
    verify(paymentStatusUpdater, never()).updateWithStatus(any(), any(), any());
  }

  @Test
  public void shouldUpdatePaymentStatusIfExternalPaymentStatusChanged() {
    // given
    ExternalPaymentStatus initialStatus = ExternalPaymentStatus.CREATED;
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.SUCCESS;
    String email = "a@b.com";
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    Payment payment = mockInternalPaymentWith(paymentId, "ext-id", initialStatus);
    mockSuccessStatusFor(payment, email);
    mockSuccessStatusUpdater(payment);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).contains(payment);
    verify(paymentStatusUpdater).updateWithStatus(eq(payment), eq(newStatus), argThat(callback -> {
      Payment paymentWithEmail = callback.apply(payment);
      return email.equals(paymentWithEmail.getEmailAddress());
    }));
  }

  private void mockSuccessStatusUpdater(Payment payment) {
    given(paymentStatusUpdater.updateWithStatus(eq(payment), any(), any()))
        .willAnswer(answer -> answer.getArgument(0));
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId,
      ExternalPaymentStatus newStatus) {
    Payment payment = TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId)
        .toBuilder()
        .externalPaymentStatus(newStatus)
        .build();
    mockInternalPaymentInDatabase(paymentId, payment);
    return payment;
  }

  private void mockInternalPaymentInDatabase(UUID paymentId, Payment payment) {
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.of(payment));
  }

  private void mockSuccessStatusFor(Payment payment, String email) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .email(email)
            .state(PaymentState.builder().status(ExternalPaymentStatus.SUCCESS.name()).build())
            .build()
        ));
  }

  private void mockExternalStatusFor(Payment payment, ExternalPaymentStatus status) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(status.name()).build())
            .build()
        ));
  }

  private void mockSameExternalStatusFor(Payment payment) {
    mockExternalStatusFor(payment, payment.getExternalPaymentStatus());
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId) {
    Payment payment = TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId);
    mockInternalPaymentInDatabase(paymentId, payment);
    return payment;
  }

  private void mockPaymentAbsenceInExternalService(Payment payment) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.empty());
  }
}