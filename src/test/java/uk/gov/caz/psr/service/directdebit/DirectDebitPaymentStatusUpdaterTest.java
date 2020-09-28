package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.PaymentUpdateStatusBuilder;
import uk.gov.caz.psr.util.TestObjectFactory.DirectDebitPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class DirectDebitPaymentFinalizerTest {

  private static final String ANY_EMAIL = "test@email.com";

  @Mock
  private PaymentUpdateStatusBuilder paymentUpdateStatusBuilder;
  @Mock
  private PaymentRepository internalPaymentsRepository;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  DirectDebitPaymentFinalizer directDebitPaymentFinalizer;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;
    DirectDebitPayment directDebitPayment = DirectDebitPayments.any();

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentFinalizer
            .finalizeSuccessfulPayment(payment, directDebitPayment.getPaymentId(), ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenDirectDebitPaymentIdIsNull() {
    // given
    Payment payment = Payments.existing();
    String directDebitPaymentId = null;

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentFinalizer
            .finalizeSuccessfulPayment(payment, directDebitPaymentId, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("directDebitPaymentId cannot be empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenDirectDebitPaymentIdIsEmpty() {
    // given
    Payment payment = Payments.existing();
    String directDebitPaymentId = "";

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentFinalizer
            .finalizeSuccessfulPayment(payment, directDebitPaymentId, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("directDebitPaymentId cannot be empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrants() {
    // given
    Payment payment = paymentWithEmptyVehicleEntrants();
    DirectDebitPayment directDebitPayment = DirectDebitPayments.any();

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentFinalizer
            .finalizeSuccessfulPayment(payment, directDebitPayment.getPaymentId(), ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Entrant payments cannot be empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenEmailNotPresent() {
    // given
    Payment payment = Payments.existing();
    DirectDebitPayment directDebitPayment = DirectDebitPayments.any();
    String email = null;

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentFinalizer
            .finalizeSuccessfulPayment(payment, directDebitPayment.getPaymentId(), email));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email address cannot be null or empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldPerformUpdateWithLinkingToExistingVehicleEntrants() {
    // given
    Payment payment = Payments.existing();
    DirectDebitPayment directDebitPayment = DirectDebitPayments.any();
    Payment newPayment = mockCallsToServices(payment, directDebitPayment);

    // when
    Payment result = directDebitPaymentFinalizer
        .finalizeSuccessfulPayment(payment, directDebitPayment.getPaymentId(), ANY_EMAIL);

    // then
    assertThat(result).isEqualTo(newPayment);

    verify(paymentUpdateStatusBuilder).buildWithExternalPaymentDetails(any(), any());
    verify(applicationEventPublisher).publishEvent(any());
    verify(internalPaymentsRepository).update(any());
  }

  private Payment paymentWithEmptyVehicleEntrants() {
    return Payments.existing()
        .toBuilder()
        .entrantPayments(Collections.emptyList())
        .build();
  }

  private Payment mockCallsToServices(Payment payment, DirectDebitPayment directDebitPayment) {
    Payment newPayment = Payments.existing().toBuilder()
        .submittedTimestamp(LocalDateTime.now())
        .build();
    ExternalPaymentDetails externalPaymentDetails = externalPaymentDetailsForDirectDebitPayment();
    given(paymentUpdateStatusBuilder.buildWithExternalPaymentDetails(any(),
        eq(externalPaymentDetails))).willReturn(newPayment);
    return newPayment;
  }

  private ExternalPaymentDetails externalPaymentDetailsForDirectDebitPayment() {
    return ExternalPaymentDetails.builder()
        .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
        .build();
  }
}