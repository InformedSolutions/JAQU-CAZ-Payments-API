package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT} table.
 */
@Value
@Builder(toBuilder = true)
public class Payment {
  UUID id;
  String externalPaymentId;
  UUID cleanZoneId;
  PaymentStatus status;
  String caseReference;
  PaymentMethod paymentMethod;
  Integer chargePaid;
  String correlationId;

  // transient field, not saved in the database
  String nextUrl;
}
