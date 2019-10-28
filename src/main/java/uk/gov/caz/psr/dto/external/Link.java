package uk.gov.caz.psr.dto.external;

import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a single links returned after payment creation in gov-uk pay.
 */
@Value
@Builder
public class Link {
  String href;
  String method;
}
