package uk.gov.caz.psr.dto.external.directdebit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Value class that represents a request to GOV UK Pay service to initiate the payment journey.
 */
@Value
@Builder
public class CreateDirectDebitPaymentRequest {
  @NonNull
  Integer amount;

  @NonNull
  String reference;

  @NonNull
  String description;

  @JsonProperty("mandate_id")
  @NonNull
  String mandateId;
}
