package uk.gov.caz.psr.controller.exception;

import lombok.Value;
import org.springframework.validation.BindingResult;

/**
 * Exception class which will be used to throw exception when request validation fails in charge
 * settlement API.
 */
@Value
public class DtoValidationException extends IllegalArgumentException {

  String vrn;
  BindingResult bindingResult;
}
