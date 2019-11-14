package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
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
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class CleanupDanglingPaymentServiceTest {

  @Mock
  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;
  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;
  @Mock
  private PaymentStatusUpdater paymentStatusUpdater;

  @InjectMocks
  private CleanupDanglingPaymentService service;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;

    // when
    Throwable throwable = catchThrowable(() -> service.processDanglingPayment(payment));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPaymentHasNotEmptyVehicleEntrants() {
    // given
    Payment payment = paymentWithNonEmptyVehicleEntrants();

    // when
    Throwable throwable = catchThrowable(() -> service.processDanglingPayment(payment));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Vehicle entrant payments should be empty");
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenPaymentIsNotFoundExternally() {
    // given
    Payment payment = paymentWithEmptyVehicleEntrants();
    mockPaymentAbsenceInExternalService(payment);

    // when
    Throwable throwable = catchThrowable(() -> service.processDanglingPayment(payment));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("External payment not found with id");
  }

  @Test
  public void shouldNotUpdateStatusIfNotChanged() {
    // given
    Payment payment = paymentWithEmptyVehicleEntrants();
    mockSameExternalStatusInExternalService(payment);

    // when
    service.processDanglingPayment(payment);

    // then
    verify(paymentStatusUpdater, never()).updateWithStatus(any(), any(), any());
    verify(vehicleEntrantPaymentRepository, never()).findByPaymentId(any());
  }

  @Test
  public void shouldUpdateStatusIfChanged() {
    // given
    ExternalPaymentStatus status = ExternalPaymentStatus.INITIATED;
    Payment payment = paymentWithEmptyVehicleEntrantsAndStatus(status);
    mockFailedExternalStatusInExternalService(payment);
    mockVehicleEntrantsFor(payment);

    // when
    service.processDanglingPayment(payment);

    // then
    verify(paymentStatusUpdater).updateWithStatus(any(), eq(ExternalPaymentStatus.FAILED), any());
  }

  private void mockVehicleEntrantsFor(Payment payment) {
    UUID id = payment.getId();
    List<VehicleEntrantPayment> vehicleEntrantPayments = Payments.forRandomDaysWithId(id)
        .getVehicleEntrantPayments();
    given(vehicleEntrantPaymentRepository.findByPaymentId(id))
        .willReturn(vehicleEntrantPayments);
  }

  private void mockFailedExternalStatusInExternalService(Payment payment) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(ExternalPaymentStatus.FAILED.name()).build())
            .build()
        ));
  }

  private void mockPaymentAbsenceInExternalService(Payment payment) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.empty());
  }

  private Payment paymentWithEmptyVehicleEntrantsAndStatus(ExternalPaymentStatus status) {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .externalPaymentStatus(status)
        .vehicleEntrantPayments(Collections.emptyList())
        .build();
  }

  private Payment paymentWithEmptyVehicleEntrants() {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .vehicleEntrantPayments(Collections.emptyList())
        .build();
  }

  private void mockSameExternalStatusInExternalService(Payment payment) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(payment.getExternalPaymentStatus().name()).build())
            .build()
        ));
  }

  private Payment paymentWithNonEmptyVehicleEntrants() {
    return TestObjectFactory.Payments.existing();
  }
}