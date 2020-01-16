package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in {@code
 * T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT} table.
 */
@Value
@Builder(toBuilder = true)
public class EntrantPayment {

  /**
   * The internal unique identifier auto-generated by the database.
   */
  UUID cleanAirZoneEntrantPaymentId;

  /**
   * Vehicle registration number.
   */
  @NonNull
  String vrn;

  /**
   * An identifier of the Clean Air Zone.
   */
  @NonNull
  UUID cleanAirZoneId;

  /**
   * A date for which the payment is being made.
   */
  @NonNull
  LocalDate travelDate;

  /**
   * An tariff code used to calculate payment. Null for entrant payments that were created before
   * a payment was made.
   */
  String tariffCode;

  /**
   * The amount of money paid for the vehicle entrant on a given date to the given CAZ.
   */
  @NonNull
  Integer charge;

  /**
   * Status of the payment.
   */
  @NonNull
  InternalPaymentStatus internalPaymentStatus;

  /**
   * A unique identifier that provides traceability between the central CAZ Service and Local
   * Authority case management systems in the event that a payment is subject to some form of
   * customer support query (e.g. a refund or chargeback).
   */
  String caseReference;

  /**
   * A flag used in scenario where an advance payment has been attempted but declined followed by
   * user abandonment.
   */
  boolean vehicleEntrantCaptured;

  /**
   * An actor who initiated the last update.
   */
  @NonNull
  EntrantPaymentUpdateActor updateActor;
}
