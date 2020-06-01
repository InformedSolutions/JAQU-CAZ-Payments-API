package uk.gov.caz.psr.dto.external.directdebit;

import lombok.Builder;
import lombok.Value;

/**
 * Value class that represents GOV UK Pay Direct Debit Payment state.
 */
@Value
@Builder
public class DirectDebitPaymentState {
  String details;

  String status;
}
