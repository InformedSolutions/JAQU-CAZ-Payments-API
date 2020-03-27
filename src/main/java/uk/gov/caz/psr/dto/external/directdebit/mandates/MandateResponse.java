package uk.gov.caz.psr.dto.external.directdebit.mandates;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Response from GOV UK Pay service with a new direct debit mandate.
 */
@Value
@Builder
public class MandateResponse {
  @JsonProperty("mandate_id")
  String mandateId;

  @JsonProperty("provider_id")
  String providerId;

  String reference;

  @JsonProperty("return_url")
  String returnUrl;

  MandateStatus state;

  @JsonProperty("_links")
  MandateLinks links;

  @JsonProperty("bank_statement_reference")
  String bankStatementReference;

  @JsonProperty("created_date")
  String createdDate;

  String description;

  @JsonProperty("payment_provider")
  String paymentProvider;
}
