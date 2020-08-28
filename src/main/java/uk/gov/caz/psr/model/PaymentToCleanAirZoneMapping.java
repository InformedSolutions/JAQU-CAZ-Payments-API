package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that stores information about payment ID and clean air zone ID which the payment is
 * associated with.
 */
@Value
@Builder(toBuilder = true)
public class PaymentToCleanAirZoneMapping {

  /**
   * ID of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * ID of the payment.
   */
  UUID paymentId;
}
