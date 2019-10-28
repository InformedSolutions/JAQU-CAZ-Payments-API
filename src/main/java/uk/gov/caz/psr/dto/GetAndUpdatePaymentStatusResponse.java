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
  SuccessFailurePaymentStatus status;

  /**
   * Creates an instance of this class based on the passed {@link Payment} instance.
   */
  public static GetAndUpdatePaymentStatusResponse from(Payment payment) {
    return new GetAndUpdatePaymentStatusResponse(payment.getId(),
        SuccessFailurePaymentStatus.from(payment.getStatus()));
  }
}
