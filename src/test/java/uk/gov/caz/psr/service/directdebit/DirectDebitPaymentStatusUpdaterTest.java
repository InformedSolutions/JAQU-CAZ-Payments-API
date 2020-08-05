package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.PaymentUpdateStatusBuilder;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.DirectDebitPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class DirectDebitPaymentStatusUpdaterTest {

  private static final String ANY_EMAIL = "test@email.com";

  @Mock
  private PaymentUpdateStatusBuilder paymentUpdateStatusBuilder;
  @Mock
  private PaymentRepository internalPaymentsRepository;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  DirectDebitPaymentStatusUpdater directDebitPaymentStatusUpdater;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;
    DirectDebitPayment directDebitPayment = DirectDebitPayments.anyWithStatus("success");

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentStatusUpdater
            .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenDirectDebitPayment() {
    // given
    Payment payment = Payments.existing();
    DirectDebitPayment directDebitPayment = null;

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentStatusUpdater
            .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("DirectDebitPayment cannot be null");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrants() {
    // given
    Payment payment = paymentWithEmptyVehicleEntrants();
    DirectDebitPayment directDebitPayment = DirectDebitPayments.anyWithStatus("success");

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentStatusUpdater
            .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Entrant payments cannot be empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenEmailNotPresent() {
    // given
    Payment payment = anyPaymentWithStatus(ExternalPaymentStatus.INITIATED, null);
    DirectDebitPayment directDebitPayment = DirectDebitPayments.anyWithStatus("success");
    String email = null;

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentStatusUpdater
            .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, email));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email address cannot be null or empty");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenNewStatusIsTheSameAsTheExistingOne() {
    // given
    Payment payment = anyPaymentWithStatus(ExternalPaymentStatus.SUCCESS, LocalDateTime.now());
    DirectDebitPayment directDebitPayment = DirectDebitPayments.anyWithStatus("success");

    // when
    Throwable throwable = catchThrowable(
        () -> directDebitPaymentStatusUpdater
            .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Status cannot be equal to the existing status");
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  public void shouldPerformUpdateWithLinkingToExistingVehicleEntrants() {
    // given
    Payment payment = anyPaymentWithStatus(ExternalPaymentStatus.INITIATED, null);
    DirectDebitPayment directDebitPayment = DirectDebitPayments.anyWithStatus("success");
    Payment newPayment = mockCallsToServices(payment, directDebitPayment);
    ExternalPaymentDetails externalPaymentDetails = externalPaymentDetailsForDirectDebitPayment(
        directDebitPayment);

    // when
    Payment result = directDebitPaymentStatusUpdater
        .updateWithDirectDebitPaymentDetails(payment, directDebitPayment, ANY_EMAIL);

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

  private Payment anyPaymentWithStatus(ExternalPaymentStatus status,
      LocalDateTime authorisedTimestamp) {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .authorisedTimestamp(authorisedTimestamp)
        .externalPaymentStatus(status)
        .build();
  }

  private Payment mockCallsToServices(Payment payment, DirectDebitPayment directDebitPayment) {
    Payment paymentWithExternalId = buildPaymentWithExternalId(payment, directDebitPayment);
    Payment newPayment = anyPaymentWithStatus(directDebitPayment.getExternalPaymentStatus(),
        LocalDateTime.now());
    ExternalPaymentDetails externalPaymentDetails = externalPaymentDetailsForDirectDebitPayment(
        directDebitPayment);
    given(paymentUpdateStatusBuilder
        .buildWithExternalPaymentDetails(paymentWithExternalId, externalPaymentDetails))
        .willReturn(newPayment);
    return newPayment;
  }

  private ExternalPaymentDetails externalPaymentDetailsForDirectDebitPayment(
      DirectDebitPayment directDebitPayment) {
    return ExternalPaymentDetails.builder()
        .externalPaymentStatus(directDebitPayment.getExternalPaymentStatus())
        .build();
  }

  private Payment buildPaymentWithExternalId(Payment payment,
      DirectDebitPayment directDebitPayment) {
    return payment.toBuilder().externalId(directDebitPayment.getPaymentId()).build();
  }
}