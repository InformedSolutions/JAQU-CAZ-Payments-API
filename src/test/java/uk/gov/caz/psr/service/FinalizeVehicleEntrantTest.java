package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;

@ExtendWith(MockitoExtension.class)
public class FinalizeVehicleEntrantTest {

  @Mock
  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  @InjectMocks
  private FinalizeVehicleEntrantService finalizeVehicleEntrantService;

  @Test
  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
    // given
    VehicleEntrant vehicleEntrant = null;

    // when
    Throwable throwable = catchThrowable(
        () -> finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Vehicle Entrant cannot be null");
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentAlreadyAssignedToVehicleEntrant() {
    // given
    VehicleEntrant vehicleEntrant = createVehicleEntrant();
    Optional<VehicleEntrantPayment> alreadyAssignedVehicleEntrantPayment = Optional
        .ofNullable(createAlreadyAssignedVehicleEntrantPayment());
    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
        alreadyAssignedVehicleEntrantPayment);

    // when
    Throwable throwable = catchThrowable(
        () -> finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessage("Payment already assigned to Entrant.");
  }

  @Test
  public void shouldUpdateVehicleEntrantPaymentWhenNotAssignedWasFound() {
    // given
    VehicleEntrant vehicleEntrant = createVehicleEntrant();
    Optional<VehicleEntrantPayment> vehicleEntrantPayment = Optional
        .ofNullable(createFoundVehicleEntrant());
    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
        vehicleEntrantPayment);

    // when
    finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant);

    // then
    verify(vehicleEntrantPaymentRepository).update(any(VehicleEntrantPayment.class));
  }

  @Test
  public void shouldNotUpdateVehicleEntrantPaymentWhenWasNotFound() {
    // given
    VehicleEntrant vehicleEntrant = createVehicleEntrant();
    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
        Optional.empty());

    // when
    finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant);

    // then
    verify(vehicleEntrantPaymentRepository, never()).update(any(VehicleEntrantPayment.class));
  }

  private VehicleEntrant createVehicleEntrant() {
    return VehicleEntrant.builder()
        .id(UUID.randomUUID())
        .cazEntryTimestamp(LocalDateTime.of(2019,11,7,22,40, 10))
        .vrn("VRN123")
        .cleanZoneId(UUID.randomUUID())
        .build();
  }

  private VehicleEntrantPayment createAlreadyAssignedVehicleEntrantPayment() {
    return createFoundVehicleEntrant().toBuilder()
        .vehicleEntrantId(UUID.randomUUID())
        .build();
  }

  private VehicleEntrantPayment createFoundVehicleEntrant() {
    return VehicleEntrantPayment.builder()
        .id(UUID.randomUUID())
        .vrn("VRN123")
        .internalPaymentStatus(InternalPaymentStatus.PAID)
        .paymentId(UUID.randomUUID())
        .chargePaid(100)
        .cleanZoneId(UUID.randomUUID())
        .travelDate(LocalDate.of(2019,11,7))
        .build();
  }

}
