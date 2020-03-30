package uk.gov.caz.psr.dto.external;

import lombok.Value;

/**
 * Value class for storing a link representation in responses from GOV UK Pay service.
 */
@Value
public class Link {
  String href;
  String method;
}
