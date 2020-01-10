package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;

/**
 * Updates the external status of a {@link Payment} instance.
 */
@Service
@AllArgsConstructor
public class PaymentStatusUpdater {

  private final PaymentWithExternalPaymentDetailsBuilder paymentWithExternalPaymentDetailsBuilder;
  //  private final PaymentRepository internalPaymentsRepository;
  private final TransientVehicleEntrantsLinker transientVehicleEntrantsLinker;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates {@code payment} with a new {@code status}.
   */
  public Payment updateWithExternalPaymentDetails(Payment payment,
      ExternalPaymentDetails externalPaymentDetails) {
    checkPreconditions(payment, externalPaymentDetails);

    Payment paymentWithNewStatus = paymentWithExternalPaymentDetailsBuilder
        .buildPaymentWithExternalPaymentDetails(payment, externalPaymentDetails);

    Payment updatedPayment = transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(
        paymentWithNewStatus);
    //    TODO: Fix with the payment updates CAZ-1716
    //    internalPaymentsRepository.update(updatedPayment);

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
   * PaymentStatusUpdater#updateWithExternalPaymentDetails(uk.gov.caz.psr.model.Payment,
   * uk.gov.caz.psr.model.ExternalPaymentDetails)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentDetails externalPaymentDetails) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(externalPaymentDetails, "ExternalPaymentDetails cannot be null");
    Preconditions.checkArgument(
        externalPaymentDetails.getExternalPaymentStatus() != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        externalPaymentDetails.getExternalPaymentStatus(), payment.getExternalPaymentStatus());
    //    TODO: Fix with the payment updates CAZ-1716
    //    Preconditions.checkArgument(!payment.getVehicleEntrantPayments().isEmpty(),
    //        "vehicle entrant payments cannot be empty");
  }
}
