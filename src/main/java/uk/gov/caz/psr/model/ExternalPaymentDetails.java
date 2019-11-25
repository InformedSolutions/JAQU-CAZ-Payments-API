package uk.gov.caz.psr.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;


/**
 * An entity which contains details of Payment in GOV.UK PAY.
 */
@Value
@Builder
public class ExternalPaymentDetails {
  /**
   * Status of payment in GOV.UK PAY
   */
  @NonNull
  ExternalPaymentStatus externalPaymentStatus;

  /**
   * Email registered in GOV.UK PAY during the payment.
   * Can be null for not finished payments.
   */
  String email;
}
