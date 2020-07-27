package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * An entity which represent a compound response from the DB from {@code PAYMENT}, {@code
 * VEHICLE_ENTRANT_PAYMENT} and {@code VEHICLE_ENTRANT_PAYMENT_MATCH} tables.
 */
@Value
@Builder(toBuilder = true)
public class PaymentSummary {

  /**
   * ID of the payment.
   */
  UUID paymentId;

  /**
   * Quantity of the payed entries to CAZ.
   */
  int entriesCount;

  /**
   * Total amount paid.
   */
  int totalPaid;

  /**
   * ID of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * ID of the payer user.
   */
  UUID payerId;

  /**
   * Date of the payment creation.
   */
  LocalDate paymentDate;
}
