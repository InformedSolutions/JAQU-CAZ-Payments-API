package uk.gov.caz.psr.service.generatecsv;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates full Csv file name that can be export to S3.
 */
@Component
public class CsvFileNameGenerator {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyyMMdd_HHmmss");

  private static final String CSV_FILE_EXT = "csv";
  private static final String PAYMENTS = "payments";
  private static final String DASH = "-";
  private static final String DOT = ".";

  /**
   * Generate full Csv file name.
   *
   * @param accountId ID of Account/Fleet.
   * @return Full Csv file name.
   */
  public String generate(UUID accountId) {
    String accountIdStripped = accountId.toString().substring(24);
    LocalDateTime now = LocalDateTime.now();
    return PAYMENTS
        + DASH
        + accountIdStripped
        + DASH
        + DATE_TIME_FORMATTER.format(now)
        + DOT
        + CSV_FILE_EXT;
  }
}