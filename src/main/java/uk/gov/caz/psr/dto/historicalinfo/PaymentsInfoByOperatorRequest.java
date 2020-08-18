package uk.gov.caz.psr.dto.historicalinfo;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Value;
import uk.gov.caz.common.util.Strings;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

/**
 * Value object that represents the request to obtain information, retrieved in a paginated manner,
 * about payments made by the given operator.
 */
@Value
public class PaymentsInfoByOperatorRequest {

  private static final int DEFAULT_PAGE_NUMBER = 0;
  private static final int DEFAULT_PAGE_SIZE = 10;

  private static final Map<Function<PaymentsInfoByOperatorRequest, Boolean>, String> validators =
      ImmutableMap.<Function<PaymentsInfoByOperatorRequest, Boolean>, String>builder()
          .put(nonNegativePageNoIfPresent(), "'pageNumber' must be non-negative")
          .put(positivePageSizeIfPresent(), "'pageSize' must be positive")
          .build();

  /**
   * An optional query param that specifies the page number.
   */
  Integer pageNumber;

  /**
   * An optional query param that specifies the page size.
   */
  Integer pageSize;

  /**
   * Validates this object alongside the passed {@code operatorId}.
   */
  public void validateWith(String operatorId) {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });

    if (!Strings.isValidUuid(operatorId)) {
      throw new InvalidRequestPayloadException("'operatorId' must be a valid UUID");
    }
  }

  /**
   * Gets the requested page number.
   */
  public int getPageNumber() {
    return Optional.ofNullable(pageNumber).orElse(DEFAULT_PAGE_NUMBER);
  }

  /**
   * Gets the requested page size.
   */
  public int getPageSize() {
    return Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE);
  }

  /**
   * Returns a lambda that verifies if 'page number' is non-negative (provided it is present).
   */
  private static Function<PaymentsInfoByOperatorRequest, Boolean> nonNegativePageNoIfPresent() {
    return request -> request.getPageNumber() >= 0;
  }

  /**
   * Returns a lambda that verifies if 'page size' is positive (provided it is present).
   */
  private static Function<PaymentsInfoByOperatorRequest, Boolean> positivePageSizeIfPresent() {
    return request -> request.getPageSize() >= 1;
  }
}
