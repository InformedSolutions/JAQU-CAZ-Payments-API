//TODO: Fix with the payment updates CAZ-1716
//package uk.gov.caz.psr.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.willDoNothing;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.caz.psr.model.Payment;
//import uk.gov.caz.psr.repository.PaymentRepository;
//import uk.gov.caz.psr.util.TestObjectFactory.Payments;
//
//@ExtendWith(MockitoExtension.class)
//class CleanupDanglingPaymentsServiceTest {
//
//  @Mock
//  private PaymentRepository paymentRepository;
//
//  @Mock
//  private CleanupDanglingPaymentService cleanupDanglingPaymentService;
//
//  @InjectMocks
//  private CleanupDanglingPaymentsService service;
//
//  @Test
//  public void shouldNotUpdateAnyPaymentsWhenThereAreNonDanglingOnes() {
//    // given
//    thereAreNoDanglingPayments();
//
//    // when
//    service.updateStatusesOfDanglingPayments();
//
//    // then
//    verify(cleanupDanglingPaymentService, never()).processDanglingPayment(any());
//  }
//
//  @Test
//  public void shouldCallServiceWhenThereAreDanglingOnes() {
//    // given
//    List<Payment> danglingPayments = thereAreDanglingPayments();
//
//    // when
//    service.updateStatusesOfDanglingPayments();
//
//    // then
//    verify(cleanupDanglingPaymentService, times(danglingPayments.size()))
//        .processDanglingPayment(any());
//  }
//
//  @Test
//  public void shouldNotBreakProcessingUponSingleFailure() {
//    // given
//    List<Payment> danglingPayments = thereAreDanglingPayments();
//    mockProcessingFailureOnSecondCall();
//
//    // when
//    Throwable throwable = catchThrowable(() -> service.updateStatusesOfDanglingPayments());
//
//    // then
//    assertThat(throwable).isNull();
//    verify(cleanupDanglingPaymentService, times(danglingPayments.size()))
//        .processDanglingPayment(any());
//  }
//
//  private void mockProcessingFailureOnSecondCall() {
//    willDoNothing()
//        .willThrow(new RuntimeException(""))
//        .willDoNothing()
//        .given(cleanupDanglingPaymentService).processDanglingPayment(any());
//  }
//
//  private List<Payment> thereAreDanglingPayments() {
//    List<Payment> payments = Arrays.asList(Payments.existing(), Payments.existing(),
//        Payments.existing());
//    given(paymentRepository.findDanglingPayments()).willReturn(payments);
//    return payments;
//  }
//
//  private void thereAreNoDanglingPayments() {
//    given(paymentRepository.findDanglingPayments()).willReturn(Collections.emptyList());
//  }
//}