package uk.gov.caz.psr.dto.external.directdebit.mandates;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Request to GOV UK Pay service to create a new direct debit mandate.
 */
@Value
@Builder
public class CreateMandateRequest {
  @NonNull
  String reference;

  @NonNull
  String description;

  @JsonProperty("return_url")
  @NonNull
  String returnUrl;
}
