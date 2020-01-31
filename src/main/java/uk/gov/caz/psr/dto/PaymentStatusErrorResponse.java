package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.psr.model.ValidationError;
import uk.gov.caz.psr.service.exception.PaymentDoesNotExistException;

/**
 * Value object that represents single error response which is returned to the client upon a call to
 * get return error with details (vrn, title, detail, status).
 */
@Value
@Builder
public class PaymentStatusErrorResponse {

  private static final String VALIDATION_ERROR_TITLE = "Validation error";
  private static final String ENTRANT_NOT_FOUND_DETAIL = 
      "A vehicle entry for the supplied combination of "
      + "vrn and date of CAZ entry could not be found";

  String vrn;
  String title;
  String field;
  String detail;
  int status = HttpStatus.BAD_REQUEST.value();

  /**
   * Static factory method.
   *
   * @param validationError An instance of {@link ValidationError} that will be mapped to {@link
   *                        PaymentStatusErrorResponse}
   * @return an instance of {@link PaymentStatusErrorResponse}
   */
  public static PaymentStatusErrorResponse from(ValidationError validationError) {
    return PaymentStatusErrorResponse.builder()
        .vrn(validationError.getVrn())
        .title(validationError.getTitle())
        .field(validationError.getField())
        .detail(validationError.getDetail())
        .build();
  }

  /**
   * Creates a validation error response, i.e. its title is fixed and equal to 'Validation error',
   * status is equal to 400 and detail is set to the parameter.
   */
  public static PaymentStatusErrorResponse validationErrorResponseWithDetailAndVrn(String vrn,
      String detail) {
    return PaymentStatusErrorResponse.builder()
        .vrn(vrn)
        .title(VALIDATION_ERROR_TITLE)
        .detail(detail)
        .build();
  }
  
  /**
   * Creates an application error response for when a vehicle entrant has not been found
   * for the parameters provided.
   */
  public static PaymentStatusErrorResponse errorResponseFromNonExistentEntrantException(
      PaymentDoesNotExistException e) {
    return PaymentStatusErrorResponse.builder()
        .vrn(e.getVrn())
        .title(e.getMessage())
        .field("vrn")
        .detail(ENTRANT_NOT_FOUND_DETAIL)
        .build();
  }
}
