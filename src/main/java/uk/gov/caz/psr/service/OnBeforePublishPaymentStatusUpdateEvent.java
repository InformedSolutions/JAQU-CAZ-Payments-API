package uk.gov.caz.psr.service;

import uk.gov.caz.psr.model.Payment;

/**
 * A functional interface that represents an action which takes place before {@link
 * uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent} is published.
 */
@FunctionalInterface
public interface OnBeforePublishPaymentStatusUpdateEvent {

  /**
   * Transforms the event's payload, i.e. an instance of {@link Payment}.
   */
  Payment apply(Payment payment);

  /**
   * Creates an instance of {@link OnBeforePublishPaymentStatusUpdateEvent} which builds a new
   * instance of {@link Payment} with the passed {@code email} address provided it is not null.
   * An identity function is returned otherwise.
   */
  static OnBeforePublishPaymentStatusUpdateEvent buildPaymentWith(String email) {
    if (email == null) {
      return payment -> payment;
    }
    return payment -> payment.toBuilder().emailAddress(email).build();
  }
}
