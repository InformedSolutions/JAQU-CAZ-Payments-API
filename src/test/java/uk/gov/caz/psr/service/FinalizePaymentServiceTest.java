package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrance;
import uk.gov.caz.psr.repository.VehicleEntranceRepository;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
public class FinalizePaymentServiceTest {

  @Mock
  private VehicleEntranceRepository vehicleEntranceRepository;

  @InjectMocks
  private FinalizePaymentService finalizePaymentService;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;

    // when
    Throwable throwable = catchThrowable(
        () -> finalizePaymentService.connectExistingVehicleEntrants(payment));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldReturnProvidedPaymentWhenStatusNotSuccess() {
    // given
    Payment payment = Payments.existing();

    // when
    Payment result = finalizePaymentService.connectExistingVehicleEntrants(payment);

    // then
    assertThat(result).isEqualTo(payment);
    verify(vehicleEntranceRepository, never()).findBy(any(), any(), any());
  }

  @Test
  public void shouldReturnProvidedPaymentWhenStatusSuccessButNoEntrantsFound() {
    // given
    Payment payment = createSuccessPayment();
    mockNotFoundVehicleEntrance();

    // when
    Payment result = finalizePaymentService.connectExistingVehicleEntrants(payment);

    // then
    assertThat(result).isEqualTo(payment);
    assertThat(result.getVehicleEntrantPayments().get(1).getVehicleEntrantId()).isNull();
    verify(vehicleEntranceRepository, times(payment.getVehicleEntrantPayments().size()))
        .findBy(any(), any(), any());
  }

  @Test
  public void shouldReturnUpdatedPaymentWhenStatusSuccessAndEntrantsFound() {
    // given
    Payment payment = createSuccessPayment();
    mockFoundVehicleEntrance();

    // when
    Payment result = finalizePaymentService.connectExistingVehicleEntrants(payment);

    // then
    assertThat(result).isNotEqualTo(payment);
    assertThat(result.getVehicleEntrantPayments().get(1).getVehicleEntrantId()).isNotNull();
    verify(vehicleEntranceRepository, times(payment.getVehicleEntrantPayments().size()))
        .findBy(any(), any(), any());
  }

  private Payment createSuccessPayment() {
    return Payments.existing().toBuilder()
        .status(PaymentStatus.SUCCESS)
        .build();
  }

  private Optional<VehicleEntrance> createVehicleEntrance() {
    return Optional.ofNullable(VehicleEntrance.builder()
        .id(UUID.randomUUID())
        .cazEntryTimestamp(LocalDateTime.now())
        .vrn("VRN123")
        .cleanZoneId(UUID.randomUUID())
        .build());
  }

  private void mockNotFoundVehicleEntrance() {
    given(vehicleEntranceRepository.findBy(any(), any(), any())).willReturn(Optional.empty());
  }

  private void mockFoundVehicleEntrance() {
    given(vehicleEntranceRepository.findBy(any(), any(), any())).willReturn(
        createVehicleEntrance()
    );
  }
}
