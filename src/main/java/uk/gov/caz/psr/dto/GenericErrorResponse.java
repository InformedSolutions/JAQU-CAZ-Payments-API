package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@Builder
public class GenericErrorResponse {
  int status = HttpStatus.BAD_REQUEST.value();
  String message;
}
