package uk.gov.caz.psr.model;

import lombok.Builder;
import lombok.Value;


@Builder
@Value
public final class ValidationError {
  String vrn;
  String title;
  String field;
  String detail;
}
