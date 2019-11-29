package uk.gov.caz.psr.model;

import lombok.Builder;
import lombok.Value;

/**
 * An entity which represents a compound response from DB from {@code PAYMENT}
 * and {@code VEHICLE_ENTRANT_PAYMENT} tables.
 */
@Value
@Builder
public class PaymentStatus {

  /**
   * The unique payment ID coming from GOV UK Pay services.
   */
  String externalId;

  /**
   * Status of the payment.
   */
  InternalPaymentStatus status;

  /**
   * A unique identifier that provides traceability between the central CAZ Service
   * and Local Authority.
   */
  String caseReference;


  private static final PaymentStatus EMPTY = PaymentStatus.builder()
      .status(InternalPaymentStatus.NOT_PAID)
      .build();

  /**
   * Helper method which returns built object with NOT_PAID status.
   *
   * @return {@link PaymentStatus}
   */
  public static PaymentStatus getEmptyPaymentStatusResponse() {
    return EMPTY;
  }

}
