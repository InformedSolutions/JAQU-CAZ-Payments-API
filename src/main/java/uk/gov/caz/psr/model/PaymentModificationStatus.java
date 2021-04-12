package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class which stores payment status fetched from payment audit table -
 * T_CLEAN_AIR_ZONE_PAYMENT_DETAIL which are required to proper information Payment modifications.
 */
@Value
@Builder
public class PaymentModificationStatus {

  /**
   * Identifier of the payment.
   */
  UUID paymentId;

  /**
   * Status of the payment.
   */
  InternalPaymentStatus paymentStatus;
}
