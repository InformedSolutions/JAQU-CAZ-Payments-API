package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class PaymentStatusUpdaterTest {

  @Mock
  private PaymentWithExternalStatusBuilder paymentWithExternalStatusBuilder;
  @Mock
  private PaymentRepository internalPaymentsRepository;
  @Mock
  private TransientVehicleEntrantsLinker transientVehicleEntrantsLinker;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  private PaymentStatusUpdater updater;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;
    ExternalPaymentStatus status = ExternalPaymentStatus.SUCCESS;

    // when
    Throwable throwable = catchThrowable(() -> updater.updateWithStatus(payment, status,
        payment1 -> payment1));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenStatusIsNull() {
    // given
    Payment payment = anyPayment();
    ExternalPaymentStatus status = null;

    // when
    Throwable throwable = catchThrowable(() -> updater.updateWithStatus(payment, status,
        payment1 -> payment1));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Status cannot be null");
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrants() {
    // given
    Payment payment = paymentWithEmptyVehicleEntrants();
    ExternalPaymentStatus status = ExternalPaymentStatus.SUCCESS;

    // when
    Throwable throwable = catchThrowable(() -> updater.updateWithStatus(payment, status,
        payment1 -> payment1));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("vehicle entrant payments cannot be empty");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenNewStatusIsTheSameAsTheExistingOne() {
    // given
    ExternalPaymentStatus initStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = anyPaymentWithStatus(initStatus);
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.INITIATED;

    // when
    Throwable throwable = catchThrowable(() -> updater.updateWithStatus(payment, newStatus,
        payment1 -> payment1));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Status cannot be equal to the existing status");
  }

  @Test
  public void shouldPerformUpdateWithLinkingToExistingVehicleEntrants() {
    // given
    ExternalPaymentStatus initStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = anyPaymentWithStatus(initStatus);
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.FAILED;
    Payment newPayment = mockCallsToServices(payment, newStatus);

    // when
    Payment result = updater.updateWithStatus(payment, newStatus, payment1 -> payment1);

    // then
    assertThat(result).isEqualTo(newPayment);

    verify(paymentWithExternalStatusBuilder).buildPaymentWithStatus(payment, newStatus);
    verify(transientVehicleEntrantsLinker).associateExistingVehicleEntrantsWith(any());
    verify(applicationEventPublisher).publishEvent(any());
    verify(internalPaymentsRepository).update(any());
  }

  private Payment paymentWithEmptyVehicleEntrants() {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .vehicleEntrantPayments(Collections.emptyList())
        .build();
  }

  private Payment mockCallsToServices(Payment payment, ExternalPaymentStatus newStatus) {
    Payment newPayment = anyPaymentWithStatus(newStatus);
    given(paymentWithExternalStatusBuilder.buildPaymentWithStatus(payment, newStatus))
        .willReturn(newPayment);
    given(transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(newPayment))
        .willReturn(newPayment);
    return newPayment;
  }

  private Payment anyPaymentWithStatus(ExternalPaymentStatus status) {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .externalPaymentStatus(status)
        .build();
  }

  private Payment anyPayment() {
    return TestObjectFactory.Payments.existing();
  }
}