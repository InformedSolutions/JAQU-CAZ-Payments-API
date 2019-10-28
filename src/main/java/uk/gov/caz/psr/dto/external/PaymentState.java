package uk.gov.caz.psr.dto.external;

import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a payment state from gov-uk pay.
 */
@Value
@Builder
public class PaymentState {
  String status;
  boolean finished;
  String message;
  String code;
}
