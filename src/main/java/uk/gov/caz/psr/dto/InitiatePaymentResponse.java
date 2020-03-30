package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Value;
import uk.gov.caz.psr.model.Payment;

/**
 * A value object that represents the response returned upon the call to initiate the payment.
 */
@Value
public class InitiatePaymentResponse {
  UUID paymentId;
  String nextUrl;

  /**
   * Creates an instance of this class based on the passed instance of {@link Payment} class.
   */
  public static InitiatePaymentResponse from(Payment payment) {
    return new InitiatePaymentResponse(payment.getId(), payment.getNextUrl());
  }
}
