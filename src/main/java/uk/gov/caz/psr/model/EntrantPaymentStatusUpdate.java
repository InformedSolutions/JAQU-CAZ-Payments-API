package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * An entity which represents a details to update payment status of {@code VehicleEntrantPayment}.
 */
@Value
@Builder
public class EntrantPaymentStatusUpdate {
  /**
   * The internal unique identifier of Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * Vehicle registration number.
   */
  String vrn;

  /**
   * Date of entry to Clean Air Zone.
   */
  LocalDate dateOfCazEntry;

  /**
   * Payment Status to be updated.
   */
  InternalPaymentStatus paymentStatus;

  /**
   * Provided Case Reference.
   */
  String caseReference;
}
