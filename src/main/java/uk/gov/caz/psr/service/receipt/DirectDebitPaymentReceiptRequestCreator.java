package uk.gov.caz.psr.service.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Populates variables for a receipt (email) for a DirectDebit payment.
 */
public class DirectDebitPaymentReceiptRequestCreator extends
    FleetPaymentReceiptRequestCreator {

  public DirectDebitPaymentReceiptRequestCreator(CurrencyFormatter currencyFormatter,
      CleanAirZoneService cleanAirZoneNameGetterService,
      ObjectMapper objectMapper, String templateId) {
    super(currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId);
  }

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.getPaymentMethod() == PaymentMethod.DIRECT_DEBIT;
  }
}