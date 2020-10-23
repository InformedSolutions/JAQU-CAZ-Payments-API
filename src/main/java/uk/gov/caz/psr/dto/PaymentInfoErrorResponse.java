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
  private static final String QUERY_DATE_RANGE_EXCEEDED = "Query date range exceeded";
  private static final String THE_REQUESTED_DATES_EXCEED_THE_MAXIMUM_PERMITTED_RANGE =
      "The requested dates exceed the maximum permitted range";

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

  /**
   * Creates a validation error response, i.e. title is equal to 'Query date range exceeded', and
   * detail is equal to 'The requested dates exceed the maximum permitted range' status is equal to
   * 400.
   */
  public static PaymentInfoErrorResponse maxDateRangeErrorResponseWithField(String field) {
    return PaymentInfoErrorResponse.builder()
        .field(field)
        .title(QUERY_DATE_RANGE_EXCEEDED)
        .detail(THE_REQUESTED_DATES_EXCEED_THE_MAXIMUM_PERMITTED_RANGE)
        .build();
  }
}
