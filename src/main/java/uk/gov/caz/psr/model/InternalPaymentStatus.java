package uk.gov.caz.psr.model;

/**
 * An internal payment status as specified in ICD.
 */
public enum InternalPaymentStatus {
  PAID,
  NOT_PAID,
  REFUNDED,
  CHARGEBACK;

  /**
   * Maps {@link ExternalPaymentStatus} to {@link ExternalPaymentStatus}.
   */
  public static InternalPaymentStatus from(ExternalPaymentStatus externalPaymentStatus) {
    if (externalPaymentStatus ==  ExternalPaymentStatus.SUCCESS) {
      return PAID;
    }
    return NOT_PAID;
  }
}
