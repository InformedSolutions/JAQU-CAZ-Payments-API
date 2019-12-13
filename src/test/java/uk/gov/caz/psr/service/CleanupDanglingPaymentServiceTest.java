package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;
import uk.gov.caz.psr.util.GetPaymentResultConverter;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.ExternalPaymentDetailsFactory;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class CleanupDanglingPaymentServiceTest {

  @Mock
  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;
  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;
  @Mock
  private PaymentStatusUpdater paymentStatusUpdater;
  @Mock
  private GetPaymentResultConverter getPaymentResultConverter;

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
    UUID cazId = UUID.randomUUID();
    Payment payment = paymentWithEmptyVehicleEntrants();
    mockVehicleEntrantsFor(payment, cazId);
    mockPaymentAbsenceInExternalService(payment, cazId);

    // when
    Throwable throwable = catchThrowable(() -> service.processDanglingPayment(payment));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("External payment not found with id");
  }

  @Test
  public void shouldNotUpdateStatusIfNotChanged() {
    // given
    UUID cazId = UUID.randomUUID();
    Payment payment = paymentWithEmptyVehicleEntrants();
    mockVehicleEntrantsFor(payment, cazId);
    mockSameExternalStatusInExternalService(payment, cazId);
    mockGetPaymentResultConverter(payment.getExternalPaymentStatus());

    // when
    service.processDanglingPayment(payment);

    // then
    verify(paymentStatusUpdater, never()).updateWithExternalPaymentDetails(any(), any());
  }

  @Test
  public void shouldUpdateStatusIfChanged() {
    // given
    UUID cazId = UUID.randomUUID();
    ExternalPaymentDetails externalPaymentDetails =
        ExternalPaymentDetailsFactory.anyWithStatus(ExternalPaymentStatus.INITIATED);
    ExternalPaymentDetails failedExternalPaymentDetails =
        ExternalPaymentDetailsFactory.anyWithStatus(ExternalPaymentStatus.FAILED);
    Payment payment =
        paymentWithEmptyVehicleEntrantsAndStatus(externalPaymentDetails.getExternalPaymentStatus());
    mockFailedExternalStatusInExternalService(payment, cazId);
    mockVehicleEntrantsFor(payment, cazId);
    mockGetPaymentResultConverter(ExternalPaymentStatus.FAILED);

    // when
    service.processDanglingPayment(payment);

    // then
    verify(paymentStatusUpdater).updateWithExternalPaymentDetails(any(),
        eq(failedExternalPaymentDetails));
  }

  @Test
  void externalPaymentRepositoryNotCalledWhenCleanAirZoneIdNotFound() {
    // given
    ExternalPaymentDetails externalPaymentDetails =
        ExternalPaymentDetailsFactory.anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment =
        paymentWithEmptyVehicleEntrantsAndStatus(externalPaymentDetails.getExternalPaymentStatus());

    Throwable throwable = catchThrowable(() -> service.processDanglingPayment(payment));
    
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("Vehicle entrant payments should not be empty");
    verify(paymentStatusUpdater, never()).updateWithExternalPaymentDetails(any(), any());

  }

  private void mockVehicleEntrantsFor(Payment payment, UUID cazIdentifier) {
    UUID id = payment.getId();
    List<VehicleEntrantPayment> vehicleEntrantPayments =
        Payments.forRandomDaysWithId(id, cazIdentifier).getVehicleEntrantPayments();
    given(vehicleEntrantPaymentRepository.findByPaymentId(id)).willReturn(vehicleEntrantPayments);
  }

  private void mockFailedExternalStatusInExternalService(Payment payment, UUID cazIdentifier) {
    given(externalPaymentsRepository.findByIdAndCazId(payment.getExternalId(), cazIdentifier))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(ExternalPaymentStatus.FAILED.name()).build())
            .email("example@email.com").build()));
  }

  private void mockPaymentAbsenceInExternalService(Payment payment, UUID cazIdentifier) {
    given(externalPaymentsRepository.findByIdAndCazId(payment.getExternalId(), cazIdentifier))
        .willReturn(Optional.empty());
  }

  private Payment paymentWithEmptyVehicleEntrantsAndStatus(ExternalPaymentStatus status) {
    return TestObjectFactory.Payments.existing().toBuilder().externalPaymentStatus(status)
        .vehicleEntrantPayments(Collections.emptyList()).build();
  }

  private Payment paymentWithEmptyVehicleEntrants() {
    return TestObjectFactory.Payments.existing().toBuilder()
        .vehicleEntrantPayments(Collections.emptyList()).build();
  }

  private void mockSameExternalStatusInExternalService(Payment payment, UUID cazIdentifier) {
    given(externalPaymentsRepository.findByIdAndCazId(payment.getExternalId(), cazIdentifier))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(payment.getExternalPaymentStatus().name()).build())
            .build()));
  }

  private Payment paymentWithNonEmptyVehicleEntrants() {
    return TestObjectFactory.Payments.existing();
  }

  private void mockGetPaymentResultConverter(ExternalPaymentStatus externalPaymentStatus) {
    given(getPaymentResultConverter.toExternalPaymentDetails(any()))
        .willReturn(ExternalPaymentDetails.builder().email("example@email.com")
            .externalPaymentStatus(externalPaymentStatus).build());
  }
}
