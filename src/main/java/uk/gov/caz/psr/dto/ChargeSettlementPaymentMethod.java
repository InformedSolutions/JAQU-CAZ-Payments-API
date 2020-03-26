package uk.gov.caz.psr.dto;

import uk.gov.caz.psr.model.PaymentMethod;

/**
 * A method of payment in GOV UK Pay service.
 */
public enum ChargeSettlementPaymentMethod {
  CREDIT_DEBIT_CARD,
  DIRECT_DEBIT;

  /**
   * Maps {@link PaymentMethod} to {@link ChargeSettlementPaymentMethod}. If {@code paymentMethod}
   * is {@code null}, {@code null} is returned.
   */
  public static ChargeSettlementPaymentMethod from(PaymentMethod paymentMethod) {
    return paymentMethod == null
        ? null
        : ChargeSettlementPaymentMethod.valueOf(paymentMethod.name());
  }
}
