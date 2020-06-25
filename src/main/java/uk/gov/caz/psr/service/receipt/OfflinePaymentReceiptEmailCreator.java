package uk.gov.caz.psr.service.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Populates variables for a receipt (email) for an offline payment .
 */
public class OfflinePaymentReceiptEmailCreator extends FleetPaymentReceiptRequestCreator {

  public OfflinePaymentReceiptEmailCreator(CurrencyFormatter currencyFormatter,
      CleanAirZoneService cleanAirZoneNameGetterService,
      ObjectMapper objectMapper, String templateId) {
    super(currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId);
  }

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.isTelephonePayment();
  }
}
