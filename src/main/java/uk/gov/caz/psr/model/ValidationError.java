package uk.gov.caz.psr.model;

import lombok.Builder;
import lombok.Value;


@Builder
@Value
public final class ValidationError {

  public static final String MANDATORY_FIELD_MISSING_ERROR = "Mandatory field missing";
  String vrn;
  String title;
  String field;

  public String getDetail() {
    return "The " + field + " " + resolveMessage();
  }

  private String resolveMessage() {
    switch (title) {
      case MANDATORY_FIELD_MISSING_ERROR:
        return "field is mandatory";
      default:
        return title;
    }
  }
}
