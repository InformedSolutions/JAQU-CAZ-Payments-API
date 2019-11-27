package uk.gov.caz.psr.dto;

import uk.gov.caz.psr.model.InternalPaymentStatus;

/**
 * Class represents charge settlement statuses.
 */
public enum ChargeSettlementPaymentStatus {
  PAID,
  NOT_PAID,
  REFUNDED,
  CHARGEBACK;

  /**
   * Maps {@link ChargeSettlementPaymentStatus} to {@link InternalPaymentStatus}.
   */
  public static ChargeSettlementPaymentStatus from(InternalPaymentStatus internalPaymentStatus) {
    return ChargeSettlementPaymentStatus.valueOf(internalPaymentStatus.toString());
  }
}


