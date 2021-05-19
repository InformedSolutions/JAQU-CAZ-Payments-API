package uk.gov.caz.psr.service.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Populates variables for a receipt (email) for a fleets payment.
 */
public class FleetPaymentReceiptRequestCreator extends CustomPaymentReceiptEmailCreator {

  public FleetPaymentReceiptRequestCreator(CurrencyFormatter currencyFormatter,
      CleanAirZoneService cleanAirZoneNameGetterService,
      ObjectMapper objectMapper, String templateId) {
    super(currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId);
  }

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.getUserId() != null;
  }

  @Override
  Map<String, Object> createPersonalisationPayload(Payment payment) {
    return ImmutableMap.<String, Object>builder()
        .put("caz", getCazName(payment))
        .put("charges", formatEntrantDatesWithVrns(payment))
        .put("external_id", payment.getExternalId())
        .put("reference", payment.getReferenceNumber().toString())
        .put("amount", toFormattedPounds(payment.getTotalPaid()))
        .build();
  }

  /**
   * Returns a list of "travelDate - vrn - charge" lines for a payment.
   *
   * @param payment the payment to fetch entrant payment information for
   * @return a list of strings in the above format
   */
  private List<String> formatEntrantDatesWithVrns(Payment payment) {
    return payment.getEntrantPayments()
        .stream()
        .sorted(Comparator.comparing(EntrantPayment::getVrn)
            .thenComparing(EntrantPayment::getTravelDate))
        .map(entrantPayment -> entrantPayment.getVrn() + " - " + formatDate(
            entrantPayment.getTravelDate()) + " - £" + toFormattedPounds(
            entrantPayment.getCharge()))
        .collect(Collectors.toList());
  }
}
