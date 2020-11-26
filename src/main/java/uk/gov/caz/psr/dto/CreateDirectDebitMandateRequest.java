package uk.gov.caz.psr.dto;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

@Value
@Builder(toBuilder = true)
@Slf4j
public class CreateDirectDebitMandateRequest {

  private static final Map<Function<CreateDirectDebitMandateRequest, Boolean>, String> validators =
      ImmutableMap.<Function<CreateDirectDebitMandateRequest, Boolean>, String>builder()
          .put(cleanAirZoneIdNotNull(), "'cleanAirZoneId' cannot be null")
          .put(returnUrlNotEmpty(), "'returnUrl' cannot be null or empty")
          .put(sessionIdNotEmpty(), "'sessionId' cannot be null or empty")
          .build();

  @ApiModelProperty(
      value = "${swagger.model.descriptions.direct-debit-mandate-create.clean-zone-id}")
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.direct-debit-mandate-create.return-url}")
  String returnUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.direct-debit-mandate-create.session-id}")
  String sessionId;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
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
   * Returns a lambda that verifies if 'clean air zone id' is not null.
   */
  private static Function<CreateDirectDebitMandateRequest, Boolean> cleanAirZoneIdNotNull() {
    return request -> Objects.nonNull(request.getCleanAirZoneId());
  }

  /**
   * Returns a lambda that verifies if 'url' is not null and not empty.
   */
  private static Function<CreateDirectDebitMandateRequest, Boolean> returnUrlNotEmpty() {
    return request -> StringUtils.hasText(request.getReturnUrl());
  }

  /**
   * Returns a lambda that verifies if 'sessionId' is not null and not empty.
   */
  private static Function<CreateDirectDebitMandateRequest, Boolean> sessionIdNotEmpty() {
    return request -> StringUtils.hasText(request.getSessionId());
  }
}
