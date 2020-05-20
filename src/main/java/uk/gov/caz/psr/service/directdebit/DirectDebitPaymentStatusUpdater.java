package uk.gov.caz.psr.service.directdebit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.PaymentUpdateStatusBuilder;

/**
 * Updates the external status of a {@link Payment} instance.
 */
@Service
@AllArgsConstructor
public class DirectDebitPaymentStatusUpdater {

  private final PaymentUpdateStatusBuilder paymentUpdateStatusBuilder;
  private final PaymentRepository internalPaymentsRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates {@code payment} with a new {@code status}.
   */
  public Payment updateWithDirectDebitPaymentDetails(Payment payment,
      DirectDebitPayment directDebitPayment, String email) {
    checkPreconditions(payment, directDebitPayment, email);

    Payment paymentWithExternalId = buildPaymentWithExternalId(payment, directDebitPayment);
    Payment updatedPayment = paymentUpdateStatusBuilder
        .buildWithExternalPaymentDetails(paymentWithExternalId,
            buildExternalPaymentDetails(directDebitPayment));

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
   * DirectDebitPaymentStatusUpdater#updateWithDirectDebitPaymentDetails(Payment,
   * DirectDebitPayment, String)}.
   */
  private void checkPreconditions(Payment payment, DirectDebitPayment directDebitPayment,
      String email) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(directDebitPayment, "DirectDebitPayment cannot be null");
    Preconditions.checkArgument(
        directDebitPayment.getExternalPaymentStatus() != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        directDebitPayment.getExternalPaymentStatus(), payment.getExternalPaymentStatus());
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Entrant payments cannot be empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(email),
        "Email address cannot be null or empty");
  }

  /**
   * Builds {@link Payment} with {@code externalId} based on {@link DirectDebitPayment}.
   */
  private Payment buildPaymentWithExternalId(Payment payment,
      DirectDebitPayment directDebitPayment) {
    return payment.toBuilder().externalId(directDebitPayment.getPaymentId()).build();
  }

  /**
   * Builds {@link ExternalPaymentDetails} from {@link DirectDebitPayment}.
   */
  private ExternalPaymentDetails buildExternalPaymentDetails(
      DirectDebitPayment directDebitPayment) {
    return ExternalPaymentDetails.builder()
        .externalPaymentStatus(directDebitPayment.getExternalPaymentStatus())
        .build();
  }
}
