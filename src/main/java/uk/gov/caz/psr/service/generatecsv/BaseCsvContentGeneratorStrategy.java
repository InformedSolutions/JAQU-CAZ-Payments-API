package uk.gov.caz.psr.service.generatecsv;

import static uk.gov.caz.psr.util.Strings.safeToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;
import uk.gov.caz.psr.util.CurrencyFormatter;

@RequiredArgsConstructor
public class BaseCsvContentGeneratorStrategy implements CsvContentGeneratorStrategy {

  private final CurrencyFormatter currencyFormatter;

  @Override
  public String[] generateCsvHeader() {
    return new String[]{CSV_HEADER};
  }

  @Override
  public List<String[]> generateCsvContent(
      List<EnrichedCsvEntrantPayment> enrichedCsvEntrantPayments) {
    List<String[]> csvRows = new ArrayList<>();

    for (EnrichedCsvEntrantPayment enrichedCsvEntrantPayment : enrichedCsvEntrantPayments) {
      csvRows.add(new String[]{getRow(enrichedCsvEntrantPayment)});
    }

    return csvRows;
  }

  private String getRow(EnrichedCsvEntrantPayment enrichedCsvEntrantPayment) {
    return String.join(",",
        safeToString(enrichedCsvEntrantPayment.getDateOfPayment()),
        safeToString(enrichedCsvEntrantPayment.getPaymentMadeBy()),
        safeToString(enrichedCsvEntrantPayment.getCazName()),
        safeToString(enrichedCsvEntrantPayment.getVrn()),
        safeToString(enrichedCsvEntrantPayment.getDateOfEntry()),
        toFormattedPounds(enrichedCsvEntrantPayment.getCharge()),
        safeToString(enrichedCsvEntrantPayment.getPaymentReference()),
        safeToString(enrichedCsvEntrantPayment.getPaymentProviderId()),
        safeToString(enrichedCsvEntrantPayment.getEntriesCount()),
        toFormattedPounds(enrichedCsvEntrantPayment.getTotalPaid()));
  }

  /**
   * Converts pennies to its string representation in pounds.
   */
  final String toFormattedPounds(int amountInPennies) {
    double amountInPounds = toPounds(amountInPennies);
    return "£" + String.format(Locale.UK, "%.2f", amountInPounds);
  }

  /**
   * Converts pennies ({@code amountInPennies}) to pounds.
   */
  final double toPounds(int amountInPennies) {
    return currencyFormatter.parsePennies(amountInPennies);
  }
}
