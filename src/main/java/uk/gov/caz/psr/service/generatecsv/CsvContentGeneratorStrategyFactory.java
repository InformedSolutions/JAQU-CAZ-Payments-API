package uk.gov.caz.psr.service.generatecsv;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Class responsible for selecting proper strategy of CSV content generation.
 */
@Component
@RequiredArgsConstructor
public class CsvContentGeneratorStrategyFactory {

  private final CurrencyFormatter currencyFormatter;

  /**
   * Based on the provided list of {@link EnrichedCsvEntrantPayment} selects strategy of CSV content
   * generation.
   *
   * @param enrichedCsvEntrantPayments list of {@link EnrichedCsvEntrantPayment}.
   * @return {@link CsvContentGeneratorStrategy}.
   */
  public CsvContentGeneratorStrategy createStrategy(
      List<EnrichedCsvEntrantPayment> enrichedCsvEntrantPayments) {
    if (hasLocalAuthoritiesStatusUpdates(enrichedCsvEntrantPayments)) {
      return new LocalAuthorityCsvContentGeneratorStrategy(currencyFormatter);
    }

    return new BaseCsvContentGeneratorStrategy(currencyFormatter);
  }

  /**
   * Method checks if any of EntrantPayments has been modified by the Local Authorities.
   */
  private boolean hasLocalAuthoritiesStatusUpdates(
      List<EnrichedCsvEntrantPayment> enrichedEntrantPayments) {
    return enrichedEntrantPayments
        .stream()
        .filter(enrichedCsvEntrantPayment -> enrichedCsvEntrantPayment.getStatus() != null)
        .anyMatch(enrichedCsvEntrantPayment -> enrichedCsvEntrantPayment.getStatus()
            .equals(InternalPaymentStatus.REFUNDED.toString())
            || enrichedCsvEntrantPayment.getStatus()
            .equals(InternalPaymentStatus.CHARGEBACK.toString()));
  }
}
