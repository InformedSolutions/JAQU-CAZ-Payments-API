package uk.gov.caz.psr.dto.validation;

public class PageNumberValidationException extends RuntimeException {

  public PageNumberValidationException(String message) {
    super(message);
  }
}