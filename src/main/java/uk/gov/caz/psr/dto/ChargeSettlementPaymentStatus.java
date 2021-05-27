package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.caz.psr.model.InternalPaymentStatus;

/**
 * Class represents charge settlement statuses.
 */
public enum ChargeSettlementPaymentStatus {
  @JsonProperty("paid")
  PAID,
  @JsonProperty("notPaid")
  NOT_PAID,
  @JsonProperty("refunded")
  REFUNDED,
  @JsonProperty("chargeback")
  CHARGEBACK,
  @JsonProperty("failed")
  FAILED;

  /**
   * Maps {@link ChargeSettlementPaymentStatus} to {@link InternalPaymentStatus}.
   */
  public static ChargeSettlementPaymentStatus from(InternalPaymentStatus internalPaymentStatus) {
    return ChargeSettlementPaymentStatus.valueOf(internalPaymentStatus.toString());
  }
}


