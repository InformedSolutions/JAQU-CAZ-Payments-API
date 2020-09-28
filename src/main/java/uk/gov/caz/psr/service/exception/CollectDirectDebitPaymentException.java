package uk.gov.caz.psr.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * An exception wrapper for signifying an exception was encountered
 * when collecting Direct Debit Payment.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CollectDirectDebitPaymentException extends ApplicationRuntimeException {

  public CollectDirectDebitPaymentException(String message) {
    super(message);
  }
}
