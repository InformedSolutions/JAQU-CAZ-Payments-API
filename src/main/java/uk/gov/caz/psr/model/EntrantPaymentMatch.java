package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * A class that represents a record from {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH} table.
 */
@Value
@Builder(toBuilder = true)
public class EntrantPaymentMatch {
  UUID id;

  UUID paymentId;

  UUID vehicleEntrantPaymentId;

  boolean latest;
}
