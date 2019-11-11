package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class CleanupDanglingPaymentsServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private UpdatePaymentWithExternalDataService updatePaymentWithExternalDataService;

  @InjectMocks
  private CleanupDanglingPaymentsService service;

  @Test
  public void shouldNotUpdateAnyPaymentsWhenThereAreNonDanglingOnes() {
    // given
    thereAreNoDanglingPayments();

    // when
    service.updateStatusesOfDanglingPayments();

    // then
    verify(updatePaymentWithExternalDataService, never()).updatePaymentWithExternalData(any());
  }

  @Test
  public void shouldCallServiceWhenThereAreDanglingOnes() {
    // given
    List<Payment> danglingPayments = thereAreDanglingPayments();

    // when
    service.updateStatusesOfDanglingPayments();

    // then
    verify(updatePaymentWithExternalDataService, times(danglingPayments.size()))
        .updatePaymentWithExternalData(any());
  }

  @Test
  public void shouldNotBreakProcessingUponSingleFailure() {
    // given
    List<Payment> danglingPayments = thereAreDanglingPayments();
    mockProcessingFailureOnSecondCall();

    // when
    Throwable throwable = catchThrowable(() -> service.updateStatusesOfDanglingPayments());

    // then
    assertThat(throwable).isNull();
    verify(updatePaymentWithExternalDataService, times(danglingPayments.size()))
        .updatePaymentWithExternalData(any());
  }

  private void mockProcessingFailureOnSecondCall() {
    given(updatePaymentWithExternalDataService.updatePaymentWithExternalData(any()))
        .willAnswer(answer -> answer.getArgument(0))
        .willThrow(new RuntimeException(""))
        .willAnswer(answer -> answer.getArgument(0));
  }

  private List<Payment> thereAreDanglingPayments() {
    List<Payment> payments = Arrays.asList(Payments.existing(), Payments.existing(),
        Payments.existing());
    given(paymentRepository.findDanglingPayments()).willReturn(payments);
    return payments;
  }

  private void thereAreNoDanglingPayments() {
    given(paymentRepository.findDanglingPayments()).willReturn(Collections.emptyList());
  }
}