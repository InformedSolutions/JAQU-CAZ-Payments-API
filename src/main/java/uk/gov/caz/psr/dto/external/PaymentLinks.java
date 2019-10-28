package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * Value class for storing links related to requests to initiate the payment journey.
 */
@Value
public class PaymentLinks {
  @JsonProperty("next_url")
  Link nextUrl;
}
