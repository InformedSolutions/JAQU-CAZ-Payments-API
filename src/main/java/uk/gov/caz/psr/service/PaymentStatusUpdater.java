package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Updates the external status of a {@link Payment} instance.
 */
@Service
@AllArgsConstructor
public class PaymentStatusUpdater {

  private final PaymentUpdateStatusBuilder paymentUpdateStatusBuilder;
  private final PaymentRepository internalPaymentsRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates {@code payment} with a new {@code status}.
   */
  public Payment updateWithExternalPaymentDetails(Payment payment,
      ExternalPaymentDetails externalPaymentDetails) {
    checkPreconditions(payment, externalPaymentDetails);

    Payment updatedPayment = paymentUpdateStatusBuilder
        .buildWithExternalPaymentDetails(payment, externalPaymentDetails);

    internalPaymentsRepository.update(updatedPayment);

    publishPaymentStatusUpdatedEvent(updatedPayment);

    return updatedPayment;
  }

  /**
   * Publishes {@link PaymentStatusUpdatedEvent} with the passed {@code payment} as a payload.
   */
  private void publishPaymentStatusUpdatedEvent(Payment payment) {
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, payment);
    applicationEventPublisher.publishEvent(event);
  }

  /**
   * Verifies passed arguments if they are valid when invoking {@link
   * PaymentStatusUpdater#updateWithExternalPaymentDetails(Payment, ExternalPaymentDetails)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentDetails externalPaymentDetails) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(externalPaymentDetails, "ExternalPaymentDetails cannot be null");
    Preconditions.checkArgument(
        externalPaymentDetails.getExternalPaymentStatus() != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        externalPaymentDetails.getExternalPaymentStatus(), payment.getExternalPaymentStatus());
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Entrant payments cannot be empty");
  }
}
