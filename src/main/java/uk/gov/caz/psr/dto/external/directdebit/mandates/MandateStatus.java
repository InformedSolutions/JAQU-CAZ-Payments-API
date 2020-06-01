package uk.gov.caz.psr.dto.external.directdebit.mandates;

import lombok.Builder;
import lombok.Value;

/**
 * Direct debit mandate status.
 */
@Value
@Builder
public class MandateStatus {
  String status;

  String details;
}
