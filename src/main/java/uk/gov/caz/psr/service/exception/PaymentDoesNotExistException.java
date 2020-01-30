package uk.gov.caz.psr.service.exception;

import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.Payment} is supposed to be updated but
 * the payment is still being processed.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@Value
public class PaymentDoesNotExistException extends ApplicationRuntimeException {
  private static final long serialVersionUID = 1L;
  String vrn;
  
  public PaymentDoesNotExistException(String vrn) {
    super("Vehicle entry not found");
    this.vrn = vrn;
  }
}
