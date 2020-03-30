package uk.gov.caz.psr.dto;

import uk.gov.caz.psr.model.ExternalPaymentStatus;

/**
 * Enum that holds two states for a payment: {@code SUCCESS} or {@code FAILURE}.
 */
public enum SuccessFailurePaymentStatus {
  SUCCESS,
  FAILURE;

  /**
   * Maps the payment status from model to this one.
   *
   * @param status An internal status of the payment.
   * @return {@link SuccessFailurePaymentStatus#SUCCESS} if {@code status} is equal to {@link
   *     ExternalPaymentStatus#SUCCESS}, {@link SuccessFailurePaymentStatus#FAILURE} otherwise.
   */
  public static SuccessFailurePaymentStatus from(ExternalPaymentStatus status) {
    if (status == ExternalPaymentStatus.SUCCESS) {
      return SUCCESS;
    }
    return FAILURE;
  }
}
