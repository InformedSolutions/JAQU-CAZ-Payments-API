package uk.gov.caz.psr.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An internal payment status as specified in ICD.
 */
public enum InternalPaymentStatus {
  PAID,
  NOT_PAID,
  REFUNDED,
  CHARGEBACK,
  FAILED;

  /**
   * Maps {@link ExternalPaymentStatus} to {@link InternalPaymentStatus}.
   */
  public static InternalPaymentStatus from(ExternalPaymentStatus externalPaymentStatus) {
    if (externalPaymentStatus ==  ExternalPaymentStatus.SUCCESS) {
      return PAID;
    }
    return NOT_PAID;
  }

  private static Set<InternalPaymentStatus> modifiedStatuses() {
    return new HashSet<>(Arrays.asList(REFUNDED, CHARGEBACK, FAILED));
  }

  /**
   * Returns information whether a status has should trigger `modification` flag when
   * there is a such a payment in history.
   */
  public boolean hasModifiedFlag() {
    return modifiedStatuses().contains(this);
  }

}
