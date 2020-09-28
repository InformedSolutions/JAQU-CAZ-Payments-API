package uk.gov.caz.psr.model.directdebit;

import com.gocardless.resources.Payment;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Value class that represents GOV UK Pay Direct Debit Payment.
 */
@Value
@Builder
@Slf4j
public class DirectDebitPayment {

  /**
   * Payment Amount.
   */
  Integer amount;

  /**
   * Payment description.
   */
  String description;

  /**
   * Payment reference.
   */
  String reference;

  /**
   * External payment ID.
   */
  String paymentId;

  /**
   * Charge Date.
   */
  String createdDate;

  /**
   * Direct Debit Mandate ID.
   */
  String mandateId;

  /**
   * Payment Status.
   */
  String status;

  /**
   * Method to build {@link DirectDebitPayment} object based GoCardlessPayment.
   */
  public static DirectDebitPayment from(Payment payment, String mandateId) {
    return DirectDebitPayment.builder()
        .paymentId(payment.getId())
        .amount(payment.getAmount())
        .description(payment.getDescription())
        .reference(payment.getReference())
        .createdDate(payment.getChargeDate())
        .mandateId(mandateId)
        .status(payment.getStatus().toString())
        .build();
  }
}
