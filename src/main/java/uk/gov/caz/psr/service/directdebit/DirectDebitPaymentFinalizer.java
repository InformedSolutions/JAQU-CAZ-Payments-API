package uk.gov.caz.psr.service.directdebit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.PaymentUpdateStatusBuilder;

/**
 * Updates the external status of a {@link Payment} instance.
 */
@Service
@AllArgsConstructor
public class DirectDebitPaymentFinalizer {

  private final PaymentUpdateStatusBuilder paymentUpdateStatusBuilder;
  private final PaymentRepository internalPaymentsRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates {@code payment} with a new {@code status}.
   */
  public Payment finalizeSuccessfulPayment(Payment payment, String directDebitPaymentId,
      String email) {
    checkPreconditions(payment, directDebitPaymentId, email);

    Payment paymentWithExternalId = buildPaymentWithExternalIdAndSubmittedTimestamp(payment,
        directDebitPaymentId);
    Payment updatedPayment = paymentUpdateStatusBuilder
        .buildWithExternalPaymentDetails(paymentWithExternalId,
            buildExternalPaymentDetailsWithSuccessPaymentStatus());

    internalPaymentsRepository.update(updatedPayment);

    publishPaymentStatusUpdatedEvent(updatedPayment, email);

    return updatedPayment;
  }

  /**
   * Publishes {@link PaymentStatusUpdatedEvent} with the passed {@code payment} as a payload.
   */
  private void publishPaymentStatusUpdatedEvent(Payment payment, String email) {
    Payment paymentWithEmail = payment.toBuilder().emailAddress(email).build();
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, paymentWithEmail);
    applicationEventPublisher.publishEvent(event);
  }

  /**
   * Verifies passed arguments if they are valid when invoking {@link
   * DirectDebitPaymentFinalizer#finalizeSuccessfulPayment(Payment, String, String)}.
   */
  private void checkPreconditions(Payment payment, String directDebitPaymentId, String email) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(directDebitPaymentId),
        "directDebitPaymentId cannot be empty");
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Entrant payments cannot be empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(email),
        "Email address cannot be null or empty");
  }

  /**
   * Builds {@link Payment} with {@code externalId} based on {@link DirectDebitPayment}.
   */
  private Payment buildPaymentWithExternalIdAndSubmittedTimestamp(Payment payment,
      String directDebitPaymentId) {
    return payment.toBuilder()
        .submittedTimestamp(LocalDateTime.now())
        .externalId(directDebitPaymentId).build();
  }

  /**
   * Builds {@link ExternalPaymentDetails} from {@link DirectDebitPayment}.
   */
  private ExternalPaymentDetails buildExternalPaymentDetailsWithSuccessPaymentStatus() {
    return ExternalPaymentDetails.builder()
        .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
        .build();
  }
}
