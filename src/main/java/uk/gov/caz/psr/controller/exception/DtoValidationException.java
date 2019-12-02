package uk.gov.caz.psr.controller.exception;

import org.springframework.validation.BindingResult;

/**
 * Exception class which will be used to throw exception when request validation fails in charge
 * settlement API.
 */
public class DtoValidationException extends IllegalArgumentException {

  private final String vrn;
  private final BindingResult bindingResult;

  public DtoValidationException(String vrn, BindingResult bindingResult) {
    this.vrn = vrn;
    this.bindingResult = bindingResult;
  }

  public String getVrn() {
    return this.vrn;
  }

  public BindingResult getBindingResult() {
    return this.bindingResult;
  }
}
