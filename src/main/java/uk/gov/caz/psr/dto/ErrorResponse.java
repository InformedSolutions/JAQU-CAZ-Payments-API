package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.psr.model.ValidationError;

/**
 * Value object that represents single error response which is returned to the client upon a call to
 * get return error with details (vrn, title, detail, status).
 */
@Value
@Builder
public class ErrorResponse {

  private static final String NO_VRN = "";
  private static final String VALIDATION_ERROR_TITLE = "Validation error";

  String vrn;
  String title;
  String detail;
  int status;

  /**
   * Static factory method.
   *
   * @param validationError An instance of {@link ValidationError} that will be mapped to {@link
   *                        ErrorResponse}
   * @return an instance of {@link ErrorResponse}
   */
  public static ErrorResponse from(ValidationError validationError) {
    return ErrorResponse.builder()
        .vrn(validationError.getVrn())
        .title(validationError.getTitle())
        .detail(validationError.getDetail())
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }

  /**
   * Creates a validation error response, i.e. its title is fixed and equal to 'Validation error',
   * status is equal to 400 and detail is set to the parameter.
   */
  public static ErrorResponse validationErrorResponseWithDetailAndVrn(String vrn, String detail) {
    return ErrorResponse.builder()
        .vrn(vrn)
        .title(VALIDATION_ERROR_TITLE)
        .detail(detail)
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }
}
