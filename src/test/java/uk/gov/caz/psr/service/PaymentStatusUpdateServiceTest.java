package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentStatusUpdate;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPaymentStatusUpdates;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
public class PaymentStatusUpdateServiceTest {

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;

  @Mock
  private PaymentRepository paymentRepository;

  @InjectMocks
  private PaymentStatusUpdateService paymentStatusUpdateService;

  @Test
  public void shouldThrowNullPointerExceptionWhenProvidedListIsNull() {
    // given
    List<EntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = null;

    // when
    Throwable throwable = catchThrowable(
        () -> paymentStatusUpdateService.process(vehicleEntrantPaymentStatusUpdatesList));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("entrantPaymentStatusUpdates cannot be null");
    verify(entrantPaymentRepository, never()).update((EntrantPayment) any());
    verify(paymentRepository, never()).findByEntrantPayment(any());
  }

  @Test
  public void shouldUpdateEntrantPaymentExceptionWhenPaymentIsMissing() {
    // given
    List<EntrantPaymentStatusUpdate> entrantPaymentStatusUpdatesList = Arrays
        .asList(EntrantPaymentStatusUpdates.any());
    mockVehicleEntrantPaymentFound();
    mockPaymentNotFound();

    // when
    paymentStatusUpdateService.process(entrantPaymentStatusUpdatesList);

    // then
    verify(entrantPaymentRepository).update((EntrantPayment) any());
  }

  @Test
  public void shouldUpdateVehicleEntrantPaymentsWhenAssociatedPaymentIsFinished() {
    // given
    List<EntrantPaymentStatusUpdate> entrantPaymentStatusUpdatesList = Arrays.asList(
        EntrantPaymentStatusUpdates.any(), EntrantPaymentStatusUpdates.any()
    );
    mockVehicleEntrantPaymentFound();
    mockPaymentFound();

    // when
    paymentStatusUpdateService.process(entrantPaymentStatusUpdatesList);

    // then
    verify(entrantPaymentRepository, times(2)).update((EntrantPayment) any());
  }

  @Test
  public void shouldUpdateWithProvidedData() {
    // given
    EntrantPaymentStatusUpdate entrantPaymentStatusUpdate = EntrantPaymentStatusUpdates.any();
    List<EntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdatesList = Arrays.asList(
        entrantPaymentStatusUpdate
    );
    EntrantPayment foundVehicleEntrantPayment = EntrantPayments.anyPaid();
    mockVehicleEntrantPaymentFoundWith(foundVehicleEntrantPayment);
    mockPaymentFound();
    EntrantPayment expectedEntrantPayment = foundVehicleEntrantPayment.toBuilder()
        .internalPaymentStatus(entrantPaymentStatusUpdate.getPaymentStatus())
        .caseReference(entrantPaymentStatusUpdate.getCaseReference())
        .updateActor(EntrantPaymentUpdateActor.LA)
        .build();
    doNothing().when(entrantPaymentRepository).update(expectedEntrantPayment);

    // when
    paymentStatusUpdateService.process(vehicleEntrantPaymentStatusUpdatesList);

    // then
    verify(entrantPaymentRepository).update(expectedEntrantPayment);
  }


  private void mockVehicleEntrantPaymentFoundWith(EntrantPayment entrantPayment) {
    given(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .willReturn(java.util.Optional.ofNullable(entrantPayment));
  }

  private void mockVehicleEntrantPaymentFound() {
    EntrantPayment entrantPayment = EntrantPayments.anyPaid();

    given(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .willReturn(java.util.Optional.ofNullable(entrantPayment));
  }

  private void mockPaymentNotFound() {
    given(paymentRepository.findByEntrantPayment(any())).willReturn(Optional.empty());
  }

  private void mockPaymentFound() {
    Payment payment = Payments.existing().toBuilder()
        .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
        .authorisedTimestamp(LocalDateTime.now())
        .build();

    given(paymentRepository.findByEntrantPayment(any())).willReturn(
        java.util.Optional.ofNullable(payment));
  }
}
