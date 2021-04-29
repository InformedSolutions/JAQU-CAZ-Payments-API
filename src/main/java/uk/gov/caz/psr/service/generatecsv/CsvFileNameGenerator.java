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
      .ofPattern("ddMMMMyyyy-HHmmss");

  private static final String CSV_FILE_EXT = "csv";
  private static final String PAYMENTS = "Payment-history-";
  private static final String DOT = ".";

  /**
   * Generate full Csv file name.
   *
   * @param accountId ID of Account/Fleet.
   * @return Full Csv file name.
   */
  public String generate(UUID accountId) {
    LocalDateTime now = LocalDateTime.now();
    return PAYMENTS
        + DATE_TIME_FORMATTER.format(now)
        + DOT
        + CSV_FILE_EXT;
  }
}
