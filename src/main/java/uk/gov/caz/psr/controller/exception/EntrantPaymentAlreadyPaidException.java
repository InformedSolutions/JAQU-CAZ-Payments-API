package uk.gov.caz.psr.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception thrown when trying to pay for the Entrant Payment that is already paid.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class EntrantPaymentAlreadyPaidException extends ApplicationRuntimeException {

  public EntrantPaymentAlreadyPaidException(String message) {
    super(message);
  }
}
