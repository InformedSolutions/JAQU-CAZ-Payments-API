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
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class GetAndUpdatePaymentsServiceTest {

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
  private FinalizePaymentService finalizePaymentService;

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
  public void shouldReturnEmptyOptionalIfPaymentIsNotFoundInDatabase() {
    // given
    UUID paymentId = UUID.fromString("a80d2cc8-f97a-11e9-9272-1b75c20437eb");
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.empty());

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

  @Test
  public void shouldThrowIllegalStateExceptionWhenPaymentWithExtIdExistsButIsNotFoundExternally() {
    // given
    UUID paymentId = UUID.fromString("1ae108cc-fb9d-11e9-8483-67f1dfc0829d");
    String externalId = "external-id-1";
    mockInternalPaymentWith(paymentId, externalId);
    mockAbsenceOfExternalPayment(externalId);

    // when
    Throwable throwable = catchThrowable(() ->
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessage("External payment not found whereas the internal one with id '%s' "
            + "and external id '%s' exists", paymentId, externalId);
  }

  @Test
  public void shouldUpdatePaymentStatusIfExternalPaymentIsFound() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    ExternalPaymentStatus externalStatus = ExternalPaymentStatus.FAILED;
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockExternalPaymentWithStatus(externalId, payment, externalStatus);
    mockFinalizePayment(payment);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    Payment internalPaymentWithExternalStatus = payment.toBuilder().externalPaymentStatus(externalStatus).build();
    assertThat(result).contains(internalPaymentWithExternalStatus);
    verify(internalPaymentsRepository).update(internalPaymentWithExternalStatus);
  }

  @Test
  public void shouldNotUpdatePaymentIfExternalPaymentIsFoundAndItsStatusMatchesInternalOne() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    ExternalPaymentStatus externalStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockExternalPaymentWithStatus(externalId, payment, externalStatus);

    // when
    Optional<Payment> result = getAndUpdatePaymentsService
        .getExternalPaymentAndUpdateStatus(paymentId);

    // then
    Payment internalPaymentWithExternalStatus = payment.toBuilder().externalPaymentStatus(externalStatus).build();
    assertThat(result).contains(internalPaymentWithExternalStatus);
    verify(internalPaymentsRepository, never()).update(any());
  }

  private void mockAbsenceOfExternalPayment(String externalId) {
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.empty());
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId) {
    Payment payment = createPayment(paymentId, externalId);
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.of(payment));
    return payment;
  }

  private Payment createPayment(UUID paymentId, String externalId) {
    return TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId);
  }

  private void mockExternalPaymentWithStatus(String externalId, Payment payment,
      ExternalPaymentStatus status) {
    GetPaymentResult externalPayment = toExternalPaymentWithStatus(payment, status);
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.of(
        externalPayment));
  }

  private void mockFinalizePayment(Payment payment) {
    given(finalizePaymentService.connectExistingVehicleEntrants(any()))
        .willAnswer(i -> i.getArguments()[0]);
  }

  private GetPaymentResult toExternalPaymentWithStatus(Payment payment, ExternalPaymentStatus status) {
    return GetPaymentResult.builder()
        .paymentId(payment.getExternalId())
        .state(PaymentState.builder().status(status.name().toLowerCase()).build())
        .build();
  }
}