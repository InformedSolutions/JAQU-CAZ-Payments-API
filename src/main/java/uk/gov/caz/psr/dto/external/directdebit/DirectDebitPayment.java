package uk.gov.caz.psr.dto.external.directdebit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Value class that represents GOV UK Pay Direct Debit Payment.
 */
@Value
@Builder
public class DirectDebitPayment {
  Integer amount;

  String description;

  String reference;

  @JsonProperty("payment_id")
  String paymentId;

  String paymentProvider;

  @JsonProperty("created_date")
  String createdDate;

  @JsonProperty("mandate_id")
  String mandateId;

  @JsonProperty("provider_id")
  String providerId;

  DirectDebitPaymentState state;
}
