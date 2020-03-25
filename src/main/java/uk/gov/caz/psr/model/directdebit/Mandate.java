package uk.gov.caz.psr.model.directdebit;

import lombok.Builder;
import lombok.Value;

/**
 * Value class representing a direct debit mandate data.
 */
@Value
@Builder(toBuilder = true)
public class Mandate {

  String id;
  String reference;
  String status;
}
