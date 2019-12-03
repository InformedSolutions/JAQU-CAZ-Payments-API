package uk.gov.caz.psr.dto;

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
}
