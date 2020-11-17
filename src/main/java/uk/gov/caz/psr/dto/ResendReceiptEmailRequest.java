package uk.gov.caz.psr.dto;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

/**
 * A value object which represents a request for resending email with a receipt.
 */
@Value
@Builder
public class ResendReceiptEmailRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.resend-receipt-email.email}")
  String email;

  private static final Map<Function<ResendReceiptEmailRequest, Boolean>, String> validators =
      ImmutableMap.<Function<ResendReceiptEmailRequest, Boolean>, String>builder()
          .put(emailIsNotBlank(), "'email' cannot be blank")
          .build();

  /**
   * Validates the given object and throws exception when validation fails.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Method validates if the provided `email` parameter is not blank.
   */
  private static Function<ResendReceiptEmailRequest, Boolean> emailIsNotBlank() {
    return request -> StringUtils.isNotBlank(request.email);
  }
}
