package uk.gov.caz.psr.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which is thrown when {@link uk.gov.caz.psr.model.Payment} does not exist.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PaymentNotFoundException extends ApplicationRuntimeException {

  public PaymentNotFoundException(String message) {
    super(message);
  }
}
