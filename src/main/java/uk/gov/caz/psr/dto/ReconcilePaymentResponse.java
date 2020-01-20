package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.NonNull;
import lombok.Value;
import uk.gov.caz.psr.model.Payment;

/**
 * Value object that represents the response which is returned to the client upon a call to get and
 * update the status.
 */
@Value
public class ReconcilePaymentResponse {
  @NonNull
  UUID paymentId;
  
  @NonNull
  Long referenceNumber;
  
  @NonNull
  String externalPaymentId;

  @NonNull
  SuccessFailurePaymentStatus status;

  String userEmail;
  
  /**
   * Creates an instance of this class based on the passed {@link Payment} instance.
   */
  public static ReconcilePaymentResponse from(Payment payment) {
    return new ReconcilePaymentResponse(payment.getId(), payment.getReferenceNumber(),
        payment.getExternalId(), 
        SuccessFailurePaymentStatus.from(payment.getExternalPaymentStatus()),
        payment.getEmailAddress());
  }
}
