package uk.gov.caz.psr.dto.external.directdebit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.psr.model.ExternalPaymentStatus;

/**
 * Value class that represents GOV UK Pay Direct Debit Payment.
 */
@Value
@Builder
@Slf4j
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

  /**
   * Converts the external status to its representation in model.
   */
  public ExternalPaymentStatus getExternalPaymentStatus() {
    String status = state.getStatus().toUpperCase();
    if (status.equals("PENDING") || status.equals("SUCCESS")) {
      return ExternalPaymentStatus.SUCCESS;
    } else {
      return ExternalPaymentStatus.ERROR;
    }
  }
}
