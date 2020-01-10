//TODO: remove if not needed after refactoring
//package uk.gov.caz.psr.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//
//import java.time.LocalDate;
//import java.util.Optional;
//import java.util.UUID;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.caz.psr.model.InternalPaymentStatus;
//import uk.gov.caz.psr.model.VehicleEntrant;
//import uk.gov.caz.psr.model.VehicleEntrantPayment;
//import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;
//import uk.gov.caz.psr.util.TestObjectFactory;
//
//@ExtendWith(MockitoExtension.class)
//public class FinalizeVehicleEntrantServiceTest {
//
//  @Mock
//  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;
//
//  @InjectMocks
//  private FinalizeVehicleEntrantService finalizeVehicleEntrantService;
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
//    // given
//    VehicleEntrant vehicleEntrant = null;
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant));
//
//    // then
//    assertThat(throwable).isInstanceOf(NullPointerException.class)
//        .hasMessage("Vehicle Entrant cannot be null");
//  }
//
//  @Test
//  public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentAlreadyAssignedToVehicleEntrant() {
//    // given
//    VehicleEntrant vehicleEntrant = createVehicleEntrant();
//    Optional<VehicleEntrantPayment> alreadyAssignedVehicleEntrantPayment = Optional
//        .ofNullable(createAlreadyAssignedVehicleEntrantPayment());
//    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
//        alreadyAssignedVehicleEntrantPayment);
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant));
//
//    // then
//    assertThat(throwable).isInstanceOf(IllegalStateException.class)
//        .hasMessageStartingWith("Payment already assigned to an entrant");
//  }
//
//  @Test
//  public void shouldUpdateVehicleEntrantPaymentWhenNotAssignedWasFound() {
//    // given
//    VehicleEntrant vehicleEntrant = createVehicleEntrant();
//    Optional<VehicleEntrantPayment> vehicleEntrantPayment = Optional
//        .ofNullable(createFoundVehicleEntrant());
//    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
//        vehicleEntrantPayment);
//
//    // when
//    Optional<VehicleEntrantPayment> result = finalizeVehicleEntrantService
//        .connectExistingVehicleEntrantPayment(vehicleEntrant);
//
//    // then
//    assertThat(result).isNotNull();
//    assertThat(result.get().getInternalPaymentStatus()).isEqualTo(InternalPaymentStatus.PAID);
//    verify(vehicleEntrantPaymentRepository).update(any(VehicleEntrantPayment.class));
//  }
//
//  @Test
//  public void shouldNotUpdateVehicleEntrantPaymentWhenWasNotFound() {
//    // given
//    VehicleEntrant vehicleEntrant = createVehicleEntrant();
//    given(vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant)).willReturn(
//        Optional.empty());
//
//    // when
//    Optional<VehicleEntrantPayment> response = finalizeVehicleEntrantService
//        .connectExistingVehicleEntrantPayment(vehicleEntrant);
//
//    // then
//    assertThat(response).isEqualTo(Optional.empty());
//    verify(vehicleEntrantPaymentRepository, never()).update(any(VehicleEntrantPayment.class));
//  }
//
//  private VehicleEntrant createVehicleEntrant() {
//    return TestObjectFactory.VehicleEntrants.forDay(LocalDate.of(2019, 11, 7));
//  }
//
//  private VehicleEntrantPayment createAlreadyAssignedVehicleEntrantPayment() {
//    return createFoundVehicleEntrant().toBuilder()
//        .vehicleEntrantId(UUID.randomUUID())
//        .build();
//  }
//
//  private VehicleEntrantPayment createFoundVehicleEntrant() {
//    return VehicleEntrantPayment.builder()
//        .id(UUID.randomUUID())
//        .vrn("VRN123")
//        .internalPaymentStatus(InternalPaymentStatus.PAID)
//        .paymentId(UUID.randomUUID())
//        .chargePaid(100)
//        .cleanZoneId(UUID.randomUUID())
//        .travelDate(LocalDate.of(2019, 11, 7))
//        .build();
//  }
//}
