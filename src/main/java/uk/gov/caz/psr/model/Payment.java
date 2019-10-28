package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT} table.
 */
@Data
@Builder(toBuilder = true)
public class Payment {
  UUID id;
  private final String externalPaymentId;
  private final UUID userId;
  private final UUID cleanZoneId;
  private final PaymentStatus status;
  private final String caseReference;
  private final String paymentMethod;
  private final Integer chargePaid;
  private final String correlationId;

  String nextUrl;
}
