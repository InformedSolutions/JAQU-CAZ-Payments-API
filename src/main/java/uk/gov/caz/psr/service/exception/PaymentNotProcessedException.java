package uk.gov.caz.psr.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.Payment} is supposed to be updated but
 * the payment is still being processed.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class PaymentNotProcessedException extends ApplicationRuntimeException {
  public PaymentNotProcessedException(String message) {
    super(message);
  }
}
