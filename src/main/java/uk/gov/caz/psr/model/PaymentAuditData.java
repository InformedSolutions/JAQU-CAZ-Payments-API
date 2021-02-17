package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class which stores data fetched from payment audit tables - T_CLEAN_AIR_ZONE_PAYMENT_DETAIL and
 * T_CLEAN_AIR_ZONE_PAYMENT_MASTER which are required to proper information about specific entrant
 * payment status.
 */
@Value
@Builder
public class PaymentAuditData {

  /**
   * Vehicle registration number.
   */
  String vrn;

  /**
   * Identifier of the payment.
   */
  UUID paymentId;

  /**
   * A date for which the payment is being made.
   */
  LocalDate travelDate;

  /**
   * An identifier of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * Status of the payment.
   */
  InternalPaymentStatus paymentStatus;
}
