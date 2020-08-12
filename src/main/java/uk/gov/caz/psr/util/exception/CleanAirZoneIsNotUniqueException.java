package uk.gov.caz.psr.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CleanAirZoneIsNotUniqueException extends ApplicationRuntimeException {

  public CleanAirZoneIsNotUniqueException(String message) {
    super(message);
  }
}