package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.psr.model.ValidationError;

/**
 * Value object that represents single payment info error response which is returned to the client
 * upon a call to get return error with details (vrn, title, detail, status).
 */
@Value
@Builder
public class PaymentInfoErrorResponse {

  String title;
  String detail;
  String field;
  int status = HttpStatus.BAD_REQUEST.value();
  
  private static final String VALIDATION_ERROR_TITLE = "Invalid search parameter";

  /**
   * Static factory method. Error Response
   *
   * @param validationError An instance of {@link ValidationError} that will be mapped to {@link
   *     PaymentInfoErrorResponse}
   * @return an instance of {@link PaymentInfoErrorResponse}
   */
  public static PaymentInfoErrorResponse from(ValidationError validationError) {
    return PaymentInfoErrorResponse.builder()
        .title(validationError.getTitle())
        .field(validationError.getField())
        .detail(validationError.getDetail())
        .build();
  }

  /**
   * Creates a validation error response, i.e. its title is fixed and equal to 'Validation error',
   * status is equal to 400 and detail is set to the parameter.
   */
  public static PaymentInfoErrorResponse validationErrorResponseWithDetailAndField(String field,
      String detail) {
    return PaymentInfoErrorResponse.builder()
        .field(field)
        .title(VALIDATION_ERROR_TITLE)
        .detail(detail)
        .build();
  }
}
