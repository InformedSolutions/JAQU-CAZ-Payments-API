package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Updates the external status of a {@link Payment} instance.
 */
@Service
@AllArgsConstructor
public class PaymentStatusUpdater {

  private final PaymentWithExternalStatusBuilder paymentWithExternalStatusBuilder;
  private final PaymentRepository internalPaymentsRepository;
  private final TransientVehicleEntrantsLinker transientVehicleEntrantsLinker;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates {@code payment} with a new {@code status}.
   */
  public Payment updateWithStatus(Payment payment, ExternalPaymentStatus status,
      OnBeforePublishPaymentStatusUpdateEvent onBeforePublishEvent) {
    checkPreconditions(payment, status, onBeforePublishEvent);

    Payment paymentWithNewStatus = paymentWithExternalStatusBuilder.buildPaymentWithStatus(
        payment, status);

    Payment updatedPayment = transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(
        paymentWithNewStatus);

    internalPaymentsRepository.update(updatedPayment);

    publishPaymentStatusUpdatedEvent(onBeforePublishEvent.apply(updatedPayment));

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
   * PaymentStatusUpdater#updateWithStatus(uk.gov.caz.psr.model.Payment,
   * uk.gov.caz.psr.model.ExternalPaymentStatus, OnBeforePublishPaymentStatusUpdateEvent)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentStatus status,
      OnBeforePublishPaymentStatusUpdateEvent onBeforePublishEvent) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(status, "Status cannot be null");
    Preconditions.checkNotNull(onBeforePublishEvent, "onBeforePublishEvent cannot be null");
    Preconditions.checkArgument(status != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        status, payment.getExternalPaymentStatus());
    Preconditions.checkArgument(!payment.getVehicleEntrantPayments().isEmpty(),
        "vehicle entrant payments cannot be empty");
  }
}
