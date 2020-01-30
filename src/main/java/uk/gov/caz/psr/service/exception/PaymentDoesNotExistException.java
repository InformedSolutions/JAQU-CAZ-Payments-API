package uk.gov.caz.psr.service.exception;

import lombok.Value;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.Payment} is supposed to be updated but
 * the payment is still being processed.
 */
@Value
public class PaymentDoesNotExistException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  String vrn;
  
  public PaymentDoesNotExistException(String vrn) {
    super("Vehicle entry not found");
    this.vrn = vrn;
  }
}
