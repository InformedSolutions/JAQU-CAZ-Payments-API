package uk.gov.caz.psr.dto;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorsResponse {

  private static final ErrorsResponse UNHANDLED_EXCEPTION_RESPONSE = new ErrorsResponse(
      Collections.singletonList(
          ErrorResponse.builder()
              .vrn("")
              .title("Unknown error")
              .detail("Internal server error")
              .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .build())
  );

  List<ErrorResponse> errors;

  /**
   * Method to return single errors response for provided vrn and provided details.
   *
   * @param vrn Vehicle Registration Number
   * @param detail Error details.
   */
  public static ErrorsResponse singleValidationErrorResponse(String vrn, String detail) {
    ErrorResponse errorResponse = ErrorResponse
        .validationErrorResponseWithDetailAndVrn(vrn, detail);
    return new ErrorsResponse(Collections.singletonList(errorResponse));
  }

  /**
   * Method to return Error Response based on provided validationErrors.
   *
   * @param validationErrors Validation Errors.
   */
  public static ErrorsResponse from(List<ErrorResponse> validationErrors) {
    return new ErrorsResponse(validationErrors);
  }
}
