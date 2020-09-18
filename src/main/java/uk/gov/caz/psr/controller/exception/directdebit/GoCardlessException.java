package uk.gov.caz.psr.controller.exception.directdebit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception thrown when any API call to GoCardless fails.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class GoCardlessException extends ApplicationRuntimeException {
  public GoCardlessException(String message) {
    super(message);
  }
}
