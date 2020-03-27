package uk.gov.caz.psr.dto.external.directdebit.mandates;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.dto.external.Link;

/**
 * Value class for storing links related to requests to create DirectDebitMandate journey.
 */
@Value
@Builder
public class MandateLinks {
  @JsonProperty("next_url")
  Link nextUrl;
}
