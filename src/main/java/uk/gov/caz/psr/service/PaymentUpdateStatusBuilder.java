package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;

/**
 * Creates a new instance of {@link Payment} used to update status of payment in DB.
 * Returns payment with the new external status set and when a mapped internal status is 'PAID'
 * then all associated entrant payments are assigned with updated attributes
 * when the status is not 'PAID' then it returns empty list of entrant payments
 * to don't update them in DB.
 */
@Service
public class PaymentUpdateStatusBuilder {

  /**
   * Creates a new instance of {@link Payment} based on the passed {@code payment} with {@code
   * newStatus} set and mapped internal one in associated entrant payments if status is PAID.
   */
  public Payment buildWithExternalPaymentDetails(Payment payment,
      ExternalPaymentDetails externalPaymentDetails) {
    checkPreconditions(payment, externalPaymentDetails);
    ExternalPaymentStatus newStatus = externalPaymentDetails.getExternalPaymentStatus();

    return payment.toBuilder()
        .externalPaymentStatus(newStatus)
        .emailAddress(externalPaymentDetails.getEmail())
        .authorisedTimestamp(getAuthorisedTimestamp(payment, newStatus))
        .entrantPayments(buildEntrantPaymentsWith(InternalPaymentStatus.from(newStatus),
            payment.getEntrantPayments())
        )
        .build();
  }

  /**
   * Verifies whether passed {@code payment} and {@status} are in valid state when calling {@link
   * PaymentUpdateStatusBuilder#buildWithExternalPaymentDetails(Payment, ExternalPaymentDetails)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentDetails externalPaymentDetails) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(externalPaymentDetails, "externalPaymentDetails cannot be null");
    Preconditions.checkArgument(
        externalPaymentDetails.getExternalPaymentStatus() != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        externalPaymentDetails.getExternalPaymentStatus(), payment.getExternalPaymentStatus());
  }

  /**
   * Creates a new list of {@link EntrantPayment} with an internal status mapped from {@code
   * status}.
   * When status is 'PAID' then it returns list of Entrant Payments with updated details.
   * When status is not 'PAID' then it returns empty array, to don't update them in DB.
   */
  private List<EntrantPayment> buildEntrantPaymentsWith(
      InternalPaymentStatus status,
      List<EntrantPayment> entrantPayments) {
    if (!status.equals(InternalPaymentStatus.PAID)) {
      return Collections.emptyList();
    }
    return entrantPayments
        .stream()
        .map(entrantPayment -> entrantPayment.toBuilder()
            .internalPaymentStatus(status)
            .updateActor(EntrantPaymentUpdateActor.USER)
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Returns {@link LocalDateTime#now()} as the authorised payment timestamp (date and time when the
   * payment has been successfully processed by GOV UK Pay) provided {@code newExternalStatus} is
   * equal to {@link ExternalPaymentStatus#SUCCESS}. Otherwise, the current value is obtained from
   * {@code payment}.
   */
  private LocalDateTime getAuthorisedTimestamp(Payment payment,
      ExternalPaymentStatus newExternalStatus) {
    if (newExternalStatus == ExternalPaymentStatus.SUCCESS) {
      return LocalDateTime.now();
    }
    return payment.getAuthorisedTimestamp();
  }
}
