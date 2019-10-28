package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a payment links returned after payment creation in gov-uk pay.
 */
@Value
@Builder
public class PaymentLinks {
  @JsonProperty("next_url")
  Link nextUrl;
}
