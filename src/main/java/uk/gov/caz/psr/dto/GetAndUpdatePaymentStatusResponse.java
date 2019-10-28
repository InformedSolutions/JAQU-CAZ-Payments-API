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
public class GetAndUpdatePaymentStatusResponse {
  @NonNull
  UUID paymentId;
  @NonNull
  PaymentStatus paymentStatus;

  /**
   * Creates an instance of this class based on the passed {@link Payment} instance.
   */
  public static GetAndUpdatePaymentStatusResponse from(Payment payment) {
    return new GetAndUpdatePaymentStatusResponse(payment.getId(),
        fromInternalStatus(payment.getStatus()));
  }

  /**
   * Maps the internal payment's status to the one returned to the REST API caller.
   */
  private static PaymentStatus fromInternalStatus(uk.gov.caz.psr.model.PaymentStatus status) {
    if (status == uk.gov.caz.psr.model.PaymentStatus.SUCCESS) {
      return PaymentStatus.PAID;
    }
    return PaymentStatus.NOT_PAID;
  }
}
