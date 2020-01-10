//TODO: Fix with the payment updates CAZ-1716
//package uk.gov.caz.psr.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//
//import java.util.Arrays;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.caz.psr.model.VehicleEntrantPayment;
//import uk.gov.caz.psr.model.VehicleEntrantPaymentStatusUpdate;
//import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;
//import uk.gov.caz.psr.service.exception.MissingVehicleEntrantPaymentException;
//import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrantPaymentStatusUpdates;
//import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrantPayments;
//
//@ExtendWith(MockitoExtension.class)
//public class PaymentStatusUpdateServiceTest {
//
//  @Mock
//  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;
//
//  @InjectMocks
//  private PaymentStatusUpdateService paymentStatusUpdateService;
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenProvidedListIsNull() {
//    // given
//    List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = null;
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> paymentStatusUpdateService.processUpdate(vehicleEntrantPaymentStatusUpdatesList));
//
//    // then
//    assertThat(throwable).isInstanceOf(NullPointerException.class)
//        .hasMessage("vehicleEntrantPaymentStatusUpdates cannot be null");
//    verify(vehicleEntrantPaymentRepository, never()).update(anyList());
//  }
//
//  @Test
//  public void shouldThrowMissingVehicleEntrantPaymentExceptionWhenVehicleEntrantPaymentNotFound() {
//    // given
//    VehicleEntrantPaymentStatusUpdate anyVehicleEntrantPaymentStatusUpdate = VehicleEntrantPaymentStatusUpdates
//        .any();
//    List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = Arrays.asList(
//        anyVehicleEntrantPaymentStatusUpdate
//    );
//    mockVehicleEntrantPaymentNotFound();
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> paymentStatusUpdateService.processUpdate(vehicleEntrantPaymentStatusUpdatesList));
//
//    // then
//    assertThat(throwable).isInstanceOf(MissingVehicleEntrantPaymentException.class)
//        .hasMessage("VehicleEntrantPayment not found for: " + anyVehicleEntrantPaymentStatusUpdate);
//    verify(vehicleEntrantPaymentRepository, never()).update(anyList());
//  }
//
//  @Test
//  public void shouldUpdateVehicleEntrantPaymentsWhenValidParams() {
//    // given
//    List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = Arrays.asList(
//        VehicleEntrantPaymentStatusUpdates.any(), VehicleEntrantPaymentStatusUpdates.any()
//    );
//    mockVehicleEntrantPaymentFound();
//    doNothing().when(vehicleEntrantPaymentRepository).update(anyList());
//
//    // when
//    paymentStatusUpdateService.processUpdate(vehicleEntrantPaymentStatusUpdatesList);
//
//    // then
//    verify(vehicleEntrantPaymentRepository).update(anyList());
//  }
//
//
//  @Test
//  public void shouldUpdateWithProvidedData() {
//    // given
//    VehicleEntrantPaymentStatusUpdate vehicleEntrantPaymentStatusUpdate = VehicleEntrantPaymentStatusUpdates
//        .any();
//    List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = Arrays.asList(
//        vehicleEntrantPaymentStatusUpdate
//    );
//    VehicleEntrantPayment foundVehicleEntrantPayment = VehicleEntrantPayments.anyPaid();
//    mockVehicleEntrantPaymentFoundWith(foundVehicleEntrantPayment);
//    List<VehicleEntrantPayment> expectedVehicleEntrantPaymentsList = Arrays.asList(
//        foundVehicleEntrantPayment.toBuilder()
//            .caseReference(vehicleEntrantPaymentStatusUpdate.getCaseReference())
//            .internalPaymentStatus(vehicleEntrantPaymentStatusUpdate.getPaymentStatus())
//            .build()
//    );
//    doNothing().when(vehicleEntrantPaymentRepository).update(expectedVehicleEntrantPaymentsList);
//
//    // when
//    paymentStatusUpdateService.processUpdate(vehicleEntrantPaymentStatusUpdatesList);
//
//    // then
//    verify(vehicleEntrantPaymentRepository).update(expectedVehicleEntrantPaymentsList);
//  }
//
//  private void mockVehicleEntrantPaymentNotFound() {
//    given(vehicleEntrantPaymentRepository
//        .findOnePaidByCazEntryDateAndExternalPaymentId(any(), any(), any())).willReturn(
//        java.util.Optional.empty());
//  }
//
//  private void mockVehicleEntrantPaymentFoundWith(VehicleEntrantPayment vehicleEntrantPayment) {
//    given(vehicleEntrantPaymentRepository
//        .findOnePaidByCazEntryDateAndExternalPaymentId(any(), any(), any())).willReturn(
//        java.util.Optional.ofNullable(vehicleEntrantPayment));
//  }
//
//  private void mockVehicleEntrantPaymentFound() {
//    VehicleEntrantPayment vehicleEntrantPayment = VehicleEntrantPayments.anyPaid();
//
//    given(vehicleEntrantPaymentRepository
//        .findOnePaidByCazEntryDateAndExternalPaymentId(any(), any(), any())).willReturn(
//        java.util.Optional.ofNullable(vehicleEntrantPayment));
//  }
//}
