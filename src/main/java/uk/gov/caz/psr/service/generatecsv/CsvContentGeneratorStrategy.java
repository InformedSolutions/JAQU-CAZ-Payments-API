package uk.gov.caz.psr.service.generatecsv;

import java.util.List;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;

/**
 * Interface for CSV content generation.
 */
public interface CsvContentGeneratorStrategy {

  /**
   * A default CSV header.
   */
  String CSV_HEADER = "Date of payment,Payment made by,"
      + "Clean Air Zone,Number plate,Dates paid for,Charge,Payment reference,"
      + "Payment ID,Days paid for,Total amount paid";

  /**
   * Method generates a header row.
   *
   * @return String with header.
   */
  String[] generateCsvHeader();

  /**
   * Method generates a CSV file content.
   *
   * @return an array of string, where each element represents a single row.
   */
  List<String[]> generateCsvContent(List<EnrichedCsvEntrantPayment> enrichedCsvEntrantPayments);

}
