package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.caz.psr.model.PaymentMethod;

/**
 * A method of payment in GOV UK Pay service.
 */
public enum ChargeSettlementPaymentMethod {

  @JsonProperty("card")
  CARD,

  @JsonProperty("direct_debit")
  DIRECT_DEBIT;

  /**
   * Maps {CreateDirectDebitPaymentServiceTest@link PaymentMethod} to {@link
   * ChargeSettlementPaymentMethod}. If {@code paymentMethod} is {@code null}, {@code null} is
   * returned.
   */
  public static ChargeSettlementPaymentMethod from(PaymentMethod paymentMethod) {
    if (paymentMethod == null) {
      return null;
    } else if (paymentMethod.equals(PaymentMethod.CREDIT_DEBIT_CARD)) {
      return ChargeSettlementPaymentMethod.CARD;
    } else {
      return ChargeSettlementPaymentMethod.DIRECT_DEBIT;
    }
  }
}
