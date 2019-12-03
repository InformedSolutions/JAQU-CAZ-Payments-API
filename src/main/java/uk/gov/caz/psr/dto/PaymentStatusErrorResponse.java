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
public class PaymentStatusErrorResponse {

  private static final String VALIDATION_ERROR_TITLE = "Validation error";

  String vrn;
  String title;
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
}
