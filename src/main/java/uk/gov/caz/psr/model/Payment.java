package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT}
 * table.
 */
@Value
@Builder(toBuilder = true)
public class Payment {

  UUID id;

  String externalPaymentId;

  UUID cleanAirZoneId;

  String cleanAirZoneName;

  @NonNull
  PaymentStatus status;

  String caseReference;

  @NonNull
  PaymentMethod paymentMethod;

  @NonNull
  Integer chargePaid;

  String correlationId;

  // transient field, not saved in the database
  String nextUrl;
}
