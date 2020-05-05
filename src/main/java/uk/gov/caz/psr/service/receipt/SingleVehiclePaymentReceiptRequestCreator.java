package uk.gov.caz.psr.service.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Populates variables for a receipt (email) for a single vehicle payment.
 */
public class SingleVehiclePaymentReceiptRequestCreator extends CustomPaymentReceiptEmailCreator {

  public SingleVehiclePaymentReceiptRequestCreator(CurrencyFormatter currencyFormatter,
      CleanAirZoneService cleanAirZoneNameGetterService,
      ObjectMapper objectMapper, String templateId) {
    super(currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId);
  }

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.getUserId() == null;
  }

  @Override
  public Map<String, Object> createPersonalisationPayload(Payment payment) {
    return ImmutableMap.<String, Object>builder()
        .put("amount", toFormattedPounds(payment.getTotalPaid()))
        .put("caz", getCazName(payment))
        .put("date", formatTravelDates(payment))
        .put("reference", payment.getReferenceNumber().toString())
        .put("vrn", payment.getEntrantPayments().iterator().next().getVrn())
        .put("external_id", payment.getExternalId())
        .build();
  }

  /**
   * Returns a list of formatted travel dates for {@code payment}.
   */
  private List<String> formatTravelDates(Payment payment) {
    return payment.getEntrantPayments()
        .stream()
        .map(EntrantPayment::getTravelDate)
        .map(this::formatDate)
        .collect(Collectors.toList());
  }
}
