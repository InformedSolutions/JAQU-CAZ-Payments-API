package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT} table.
 */
@Data
@Builder
public class Payment {
  String externalPaymentId;
  String status;

  private final UUID id;
  private final UUID userId;
  private final UUID cleanZoneId;
  private final String caseReference;
  private final String paymentMethod;
  private final Integer chargePaid;
  private final String correlationId;

  String nextUrl;
}
