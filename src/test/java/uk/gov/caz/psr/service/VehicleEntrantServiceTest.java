//TODO: remove if not needed after refactoring
//package uk.gov.caz.psr.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
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
//import uk.gov.caz.psr.repository.VehicleEntrantRepository;
//import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrantPayments;
//import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrants;
//
//@ExtendWith(MockitoExtension.class)
//class VehicleEntrantServiceTest {
//
//  @Mock
//  private VehicleEntrantRepository vehicleEntrantRepository;
//
//  @Mock
//  private FinalizeVehicleEntrantService finalizeVehicleEntrantService;
//
//  @InjectMocks
//  private VehicleEntrantService vehicleEntrantService;
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
//    // given
//    VehicleEntrant vehicleEntrant = null;
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> vehicleEntrantService.registerVehicleEntrant(vehicleEntrant));
//
//    // then
//    assertThat(throwable).isInstanceOf(NullPointerException.class)
//        .hasMessage("Vehicle entrant cannot be null");
//  }
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIdIsNotNull() {
//    // given
//    UUID id = UUID.fromString("1ada0539-7528-456e-95eb-f14025792889");
//    VehicleEntrant vehicleEntrant = VehicleEntrants.sampleEntrantWithId(id);
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> vehicleEntrantService.registerVehicleEntrant(vehicleEntrant));
//
//    // then
//    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
//        .hasMessage("ID of the vehicle entrant must be null, actual: " + id);
//  }
//
//  @Test
//  public void shouldCallRepositoryAndReturnNotPaidPaymentStatusWhenVehicleEntrantExitingInDBIsValidButNotPaid() {
//    // given
//    VehicleEntrant vehicleEntrant = VehicleEntrants.anyWithoutId();
//    mockExistingVehicleEntrantInDatabase(vehicleEntrant);
//    mockNotPaidVehicleEntrantPayment(vehicleEntrant);
//
//    // when
//    InternalPaymentStatus response = vehicleEntrantService.registerVehicleEntrant(vehicleEntrant);
//
//    // then
//    assertThat(response).isEqualTo(InternalPaymentStatus.NOT_PAID);
//    verify(vehicleEntrantRepository).insertIfNotExists(vehicleEntrant);
//  }
//
//  @Test
//  public void shouldCallRepositoryAndReturnNotPaidPaymentStatusWhenVehicleEntrantIsValidAndPaid() {
//    // given
//    VehicleEntrant vehicleEntrant = VehicleEntrants.anyWithoutId();
//    mockAbsenceOfVehicleEntrantInDatabase(vehicleEntrant);
//    mockPaidVehicleEntrantPayment(vehicleEntrant);
//
//    // when
//    InternalPaymentStatus response = vehicleEntrantService.registerVehicleEntrant(vehicleEntrant);
//
//    // then
//    assertThat(response).isEqualTo(InternalPaymentStatus.PAID);
//    verify(vehicleEntrantRepository).insertIfNotExists(vehicleEntrant);
//    verify(finalizeVehicleEntrantService).connectExistingVehicleEntrantPayment(vehicleEntrant);
//  }
//
//  @Test
//  public void shouldCallRepositoryAndServiceWhenVehicleEntrantIsValidAndNotExistInDatabase() {
//    // given
//    VehicleEntrant vehicleEntrant = VehicleEntrants.anyWithoutId();
//    mockAbsenceOfVehicleEntrantInDatabase(vehicleEntrant);
//
//    // when
//    vehicleEntrantService.registerVehicleEntrant(vehicleEntrant);
//
//    // then
//    verify(vehicleEntrantRepository).insertIfNotExists(vehicleEntrant);
//    verify(finalizeVehicleEntrantService).connectExistingVehicleEntrantPayment(vehicleEntrant);
//  }
//
//  @Test
//  public void shouldThrowIllegalStateExceptionWhenVehicleEntrantIsNotFoundAndCannotBeInserted() {
//    // given
//    VehicleEntrant vehicleEntrant = VehicleEntrants.anyWithoutId();
//    mockIllegalStateWhenBothDbOperationsFails();
//
//    // when
//    Throwable throwable = catchThrowable(() ->
//        vehicleEntrantService.registerVehicleEntrant(vehicleEntrant));
//
//    // then
//    assertThat(throwable).isInstanceOf(IllegalStateException.class)
//        .hasMessageStartingWith("Cannot find the existing");
//  }
//
//  private void mockPaidVehicleEntrantPayment(VehicleEntrant vehicleEntrant) {
//    Optional<VehicleEntrantPayment> foundVehicleEntrantPayment = Optional
//        .of(VehicleEntrantPayments.anyPaid());
//    given(finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant))
//        .willReturn(foundVehicleEntrantPayment);
//  }
//
//  private void mockNotPaidVehicleEntrantPayment(VehicleEntrant vehicleEntrant) {
//    Optional<VehicleEntrantPayment> foundVehicleEntrantPayment = Optional
//        .of(VehicleEntrantPayments.anyNotPaid());
//    given(finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant))
//        .willReturn(foundVehicleEntrantPayment);
//  }
//
//  private void mockAbsenceOfVehicleEntrantInDatabase(VehicleEntrant vehicleEntrant) {
//    given(vehicleEntrantRepository.insertIfNotExists(vehicleEntrant))
//        .willReturn(Optional.of(vehicleEntrant));
//  }
//
//  private void mockIllegalStateWhenBothDbOperationsFails() {
//    given(vehicleEntrantRepository.insertIfNotExists(any())).willReturn(Optional.empty());
//    given(vehicleEntrantRepository.findBy(any(), any(), any())).willReturn(Optional.empty());
//  }
//
//  private void mockExistingVehicleEntrantInDatabase(VehicleEntrant vehicleEntrant) {
//    given(vehicleEntrantRepository.insertIfNotExists(vehicleEntrant)).willReturn(Optional.empty());
//    given(vehicleEntrantRepository.findBy(
//        vehicleEntrant.getCazEntryDate(),
//        vehicleEntrant.getCleanZoneId(),
//        vehicleEntrant.getVrn())
//    ).willReturn(Optional.of(vehicleEntrant));
//  }
//}