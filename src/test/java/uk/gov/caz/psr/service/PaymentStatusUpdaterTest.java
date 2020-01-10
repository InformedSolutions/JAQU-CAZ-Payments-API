//TODO: Fix with the payment updates CAZ-1716
//package uk.gov.caz.psr.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
//import java.util.Collections;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.context.ApplicationEventPublisher;
//import uk.gov.caz.psr.model.ExternalPaymentDetails;
//import uk.gov.caz.psr.model.ExternalPaymentStatus;
//import uk.gov.caz.psr.model.Payment;
//import uk.gov.caz.psr.repository.PaymentRepository;
//import uk.gov.caz.psr.util.TestObjectFactory;
//import uk.gov.caz.psr.util.TestObjectFactory.ExternalPaymentDetailsFactory;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentStatusUpdaterTest {
//
//  @Mock
//  private PaymentWithExternalPaymentDetailsBuilder paymentWithExternalPaymentDetailsBuilder;
//  @Mock
//  private PaymentRepository internalPaymentsRepository;
//  @Mock
//  private TransientVehicleEntrantsLinker transientVehicleEntrantsLinker;
//  @Mock
//  private ApplicationEventPublisher applicationEventPublisher;
//
//  @InjectMocks
//  private PaymentStatusUpdater updater;
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
//    // given
//    Payment payment = null;
//    ExternalPaymentDetails externalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.SUCCESS);
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> updater.updateWithExternalPaymentDetails(payment, externalPaymentDetails));
//
//    // then
//    assertThat(throwable).isInstanceOf(NullPointerException.class)
//        .hasMessage("Payment cannot be null");
//  }
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenStatusIsNull() {
//    // given
//    Payment payment = anyPayment();
//    ExternalPaymentDetails externalPaymentDetails = null;
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> updater.updateWithExternalPaymentDetails(payment, externalPaymentDetails));
//
//    // then
//    assertThat(throwable).isInstanceOf(NullPointerException.class)
//        .hasMessage("ExternalPaymentDetails cannot be null");
//  }
//
//  @Test
//  public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrants() {
//    // given
//    Payment payment = paymentWithEmptyVehicleEntrants();
//    ExternalPaymentDetails externalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.SUCCESS);
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> updater.updateWithExternalPaymentDetails(payment, externalPaymentDetails));
//
//    // then
//    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
//        .hasMessage("vehicle entrant payments cannot be empty");
//  }
//
//  @Test
//  public void shouldThrowNullPointerExceptionWhenNewStatusIsTheSameAsTheExistingOne() {
//    // given
//    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.INITIATED);
//    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
//    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.INITIATED);
//
//    // when
//    Throwable throwable = catchThrowable(
//        () -> updater.updateWithExternalPaymentDetails(payment, newExternalPaymentDetails));
//
//    // then
//    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
//        .hasMessageStartingWith("Status cannot be equal to the existing status");
//  }
//
//  @Test
//  public void shouldPerformUpdateWithLinkingToExistingVehicleEntrants() {
//    // given
//    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.INITIATED);
//    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
//    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
//        .anyWithStatus(ExternalPaymentStatus.FAILED);
//    Payment newPayment = mockCallsToServices(payment, newExternalPaymentDetails);
//
//    // when
//    Payment result = updater.updateWithExternalPaymentDetails(payment, newExternalPaymentDetails);
//
//    // then
//    assertThat(result).isEqualTo(newPayment);
//
//    verify(paymentWithExternalPaymentDetailsBuilder)
//        .buildPaymentWithExternalPaymentDetails(payment, newExternalPaymentDetails);
//    verify(transientVehicleEntrantsLinker).associateExistingVehicleEntrantsWith(any());
//    verify(applicationEventPublisher).publishEvent(any());
//    verify(internalPaymentsRepository).update(any());
//  }
//
//  private Payment paymentWithEmptyVehicleEntrants() {
//    return TestObjectFactory.Payments.existing()
//        .toBuilder()
//        .vehicleEntrantPayments(Collections.emptyList())
//        .build();
//  }
//
//  private Payment mockCallsToServices(Payment payment,
//      ExternalPaymentDetails newExternalPaymentDetails) {
//    Payment newPayment = anyPaymentWithStatus(newExternalPaymentDetails.getExternalPaymentStatus());
//    given(paymentWithExternalPaymentDetailsBuilder
//        .buildPaymentWithExternalPaymentDetails(payment, newExternalPaymentDetails))
//        .willReturn(newPayment);
//    given(transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(newPayment))
//        .willReturn(newPayment);
//    return newPayment;
//  }
//
//  private Payment anyPaymentWithStatus(ExternalPaymentStatus status) {
//    return TestObjectFactory.Payments.existing()
//        .toBuilder()
//        .externalPaymentStatus(status)
//        .build();
//  }
//
//  private Payment anyPayment() {
//    return TestObjectFactory.Payments.existing();
//  }
//}