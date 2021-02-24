package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in Payment Audit tables.
 */
@Value
@Builder(toBuilder = true)
public class PaymentModification {

  /**
   * Payment detail amount.
   */
  int amount;

  /**
   * Date for which the vehicle has paid to travel in to the CAZ.
   */
  LocalDate travelDate;

  /**
   * Vehicle registration number.
   */
  @ToString.Exclude
  String vrn;

  /**
   * A unique identifier that provides traceability between the central CAZ Service and Local
   * Authority.
   */
  String caseReference;

  /**
   * Timestamp of modification.
   */
  LocalDateTime modificationTimestamp;

  /**
   * Payment Status of created Vehicle Entrant.
   */
  String entrantPaymentStatus;
}
