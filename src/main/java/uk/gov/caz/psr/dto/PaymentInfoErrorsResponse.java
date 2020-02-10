package uk.gov.caz.psr.dto;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Value object that represents collection of payment info errors response which are returned to the
 * client upon a call to notify about the error.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentInfoErrorsResponse {

  List<PaymentInfoErrorResponse> errors;

  /**
   * Method to return PaymentInfoErrorsResponse based on provided validationErrors.
   *
   * @param validationErrors Validation Errors.
   */
  public static PaymentInfoErrorsResponse from(List<PaymentInfoErrorResponse> validationErrors) {
    return new PaymentInfoErrorsResponse(validationErrors);
  }
  

  /**
   * Method to return single errors response for a specific field.
   *
   * @param field the invalid field
   * @param detail Error details.
   */
  public static PaymentInfoErrorsResponse singleValidationErrorResponse(String field,
      String detail) {
    PaymentInfoErrorResponse errorResponse = PaymentInfoErrorResponse
        .validationErrorResponseWithDetailAndField(field, detail);
    return new PaymentInfoErrorsResponse(Collections.singletonList(errorResponse));
  }
}
