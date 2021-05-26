package uk.gov.caz.psr.dto.historicalinfo;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

/**
 * Value object that represents the request to obtain information, retrieved in a paginated manner,
 * about payments made by the given operator.
 */
@Value
public class PaymentsInfoByDatesRequest {

  private static final int DEFAULT_PAGE_NUMBER = 0;
  private static final int DEFAULT_PAGE_SIZE = 10;
  public static final String DATE_FORMAT = "yyyy-MM-dd";

  private static final Map<Function<PaymentsInfoByDatesRequest, Boolean>, String> validators =
      ImmutableMap.<Function<PaymentsInfoByDatesRequest, Boolean>, String>builder()
          .put(nonNegativePageNoIfPresent(), "'pageNumber' must be non-negative")
          .put(positivePageSizeIfPresent(), "'pageSize' must be positive")
          .put(request -> request.startDate != null, "'startDate' cannot be null.")
          .put(request -> request.endDate != null, "'endDate' cannot be null.")
          .put(startBeforeEndDate(), "'startDate' need to be before 'endDate'")
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
   * ISO 8601 formatted date string indicating the payments history date from.
   */
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate startDate;

  /**
   * ISO 8601 formatted date string indicating the payments history date to.
   */
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate endDate;

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
  private static Function<PaymentsInfoByDatesRequest, Boolean> nonNegativePageNoIfPresent() {
    return request -> request.getPageNumber() >= 0;
  }

  /**
   * Returns a lambda that verifies if 'page size' is positive (provided it is present).
   */
  private static Function<PaymentsInfoByDatesRequest, Boolean> positivePageSizeIfPresent() {
    return request -> request.getPageSize() >= 1;
  }

  /**
   * Helper method to convert dates to GMT.
   *
   * @return {@link LocalDateTime}
   */
  public LocalDateTime getLocalStartDate() {
    return toLocalDateTime(this.getStartDate(), LocalTime.MIDNIGHT);
  }

  /**
   * Helper method to convert dates  to GMT.
   *
   * @return {@link LocalDateTime}
   */
  public LocalDateTime getLocalEndDate() {
    return toLocalDateTime(this.getEndDate(), LocalTime.MIDNIGHT.minusNanos(1));
  }

  /**
   * Convert date and time to {@link LocalDateTime}.
   */
  private static LocalDateTime toLocalDateTime(LocalDate date, LocalTime localTime) {
    return LocalDateTime.of(date, localTime).atZone(ZoneId.of("Europe/London"))
        .withZoneSameInstant(ZoneId.of("GMT")).toLocalDateTime();
  }

  /**
   * Returns a lambda that verifies if 'start date' is before 'end date.
   */
  private static Function<PaymentsInfoByDatesRequest, Boolean> startBeforeEndDate() {
    return request -> request.getStartDate().isBefore(request.getEndDate())
        || request.getStartDate().equals(request.getEndDate());
  }
}
