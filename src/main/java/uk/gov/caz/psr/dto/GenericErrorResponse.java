package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * A value object that represents a payload of an error response, e.g. when the validation of a
 * request header fails.
 */
@Value
@Builder
public class GenericErrorResponse {
  int status = HttpStatus.BAD_REQUEST.value();
  String message;
}
