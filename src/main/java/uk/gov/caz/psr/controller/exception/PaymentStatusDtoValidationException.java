package uk.gov.caz.psr.controller.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.validation.BindingResult;

/**
 * Exception class which will be used to throw exception when payment-status request validation
 * fails in charge settlement API.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PaymentStatusDtoValidationException extends IllegalArgumentException {

  String vrn;
  String genericValidationCode;
  BindingResult bindingResult;
}
