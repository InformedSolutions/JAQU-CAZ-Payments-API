package uk.gov.caz.psr.dto.directdebit;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

/**
 * Transport object representing the payload of a request to complete the process of the
 * direct debit creation.
 */
@Value
@Builder
public class CompleteMandateCreationRequest {

  private static final Map<Function<CompleteMandateCreationRequest, Boolean>, String> validators =
      ImmutableMap.<Function<CompleteMandateCreationRequest, Boolean>, String>builder()
      .put(sessionTokenNotEmpty(), "'sessionToken' cannot be null or empty")
      .put(cazIdShouldBeValidUuid(), "'cleanAirZoneId' should be a valid UUID")
      .build();

  @ApiModelProperty(
      value = "${swagger.model.descriptions.complete-direct-debit-mandate-creation.session-token}")
  String sessionToken;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.complete-direct-debit-mandate-creation.caz-id}")
  String cleanAirZoneId;

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
   * Returns a lambda that verifies if 'session token' is not null and contains any text.
   */
  private static Function<CompleteMandateCreationRequest, Boolean> sessionTokenNotEmpty() {
    return request -> StringUtils.hasText(request.getSessionToken());
  }

  /**
   * Returns a lambda that verifies if 'cleanAirZoneId' is a valid UUID.
   */
  private static Function<CompleteMandateCreationRequest, Boolean> cazIdShouldBeValidUuid() {
    return request -> StringUtils.hasText(request.getCleanAirZoneId())
        && isValidUuid(request.getCleanAirZoneId());
  }

  /**
   * Verifies whether the passed {@code input} is a valid UUID.
   */
  private static boolean isValidUuid(String input) {
    try {
      // see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8159339
      return UUID.fromString(input).toString().equals(input);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
