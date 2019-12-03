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
  int status = HttpStatus.BAD_REQUEST.value();

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
        .detail(validationError.getDetail())
        .build();
  }
}
