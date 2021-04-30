package uk.gov.caz.psr.service.generatecsv;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generates writer with csv content.
 */
@Component
@AllArgsConstructor
public class CsvWriter {

  private final CsvContentGenerator csvContentGenerator;

  /**
   * Create {@link Writer} with content of csv.
   *
   * @param accountId ID of Account.
   * @param accountUserIds List of account user ids for which we should generate payment history.
   * @return {@link Writer}.
   */
  public Writer createWriterWithCsvContent(UUID accountId, List<UUID> accountUserIds)
      throws IOException {
    try (Writer writer = new StringWriter();
        ICSVWriter csvWriter = new CSVWriterBuilder(writer)
            .withSeparator(CSVWriter.NO_QUOTE_CHARACTER)
            .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
            .build()) {

      // Write UTF-8 BOM
      writer.write('\ufeff');

      List<String[]> csvRows = csvContentGenerator.generateCsvRows(accountId, accountUserIds);

      csvWriter.writeAll(csvRows);
      return writer;
    }
  }
}
