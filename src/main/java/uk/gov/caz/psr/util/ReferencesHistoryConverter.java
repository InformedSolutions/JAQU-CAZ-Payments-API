package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse.VehicleEntrantPaymentDetails;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.util.exception.CleanAirZoneIsNotUniqueException;

/**
 * A utility class that converts {@link Payment} into {@link ReferencesHistoryResponse}.
 */
@Component
@AllArgsConstructor
public class ReferencesHistoryConverter {

  private final CurrencyFormatter currencyFormatter;
  private final VccsRepository vccsRepository;

  /**
   * Converts the passed {@link Payment} into an instance of {@link ReferencesHistoryResponse}.
   *
   * @param payment {@link Payment}
   * @return An instance of {@link ReferencesHistoryResponse}.
   */
  public ReferencesHistoryResponse toReferencesHistoryResponse(Payment payment) {
    checkIfCazIsUnique(payment);

    UUID cleanAirZoneId = payment.getEntrantPayments().stream()
        .map(EntrantPayment::getCleanAirZoneId).findFirst().orElse(null);

    return ReferencesHistoryResponse.builder()
        .paymentReference(payment.getReferenceNumber())
        .paymentProviderId(payment.getExternalId())
        .paymentTimestamp(payment.getSubmittedTimestamp())
        .totalPaid(currencyFormatter.parsePenniesToBigDecimal(payment.getTotalPaid()))
        .telephonePayment(payment.isTelephonePayment())
        .operatorId(payment.getOperatorId())
        .cazName(getCleanAirZoneName(cleanAirZoneId))
        .paymentProviderStatus(payment.getExternalPaymentStatus())
        .lineItems(toVehicleEntrantPaymentsDetails(payment.getEntrantPayments()))
        .build();
  }

  private void checkIfCazIsUnique(Payment payment) {
    boolean isUniqueCaz = payment.getEntrantPayments()
        .stream()
        .map(EntrantPayment::getCleanAirZoneId)
        .distinct()
        .limit(2)
        .count() <= 1;

    if (!isUniqueCaz) {
      throw new CleanAirZoneIsNotUniqueException(
          "There is more than one CAZ in entrant payments. "
              + "CAZ should be unique for the same payment.");
    }
  }

  private List<VehicleEntrantPaymentDetails> toVehicleEntrantPaymentsDetails(
      List<EntrantPayment> entrantPaymentInfoList) {
    return entrantPaymentInfoList.stream()
        .map(this::toVehicleEntrantPaymentDetails)
        .collect(toList());
  }

  private ReferencesHistoryResponse.VehicleEntrantPaymentDetails toVehicleEntrantPaymentDetails(
      EntrantPayment entrantPayment) {
    return ReferencesHistoryResponse.VehicleEntrantPaymentDetails.builder()
        .chargePaid(currencyFormatter.parsePenniesToBigDecimal(
            entrantPayment.getCharge()))
        .travelDate(entrantPayment.getTravelDate())
        .vrn(entrantPayment.getVrn())
        .build();
  }

  private String getCleanAirZoneName(UUID cleanAirZone) {
    Response<CleanAirZonesDto> cleanAirZonesResponse = vccsRepository.findCleanAirZonesSync();
    return cleanAirZonesResponse.body().getCleanAirZones().stream()
        .filter(caz -> caz.getCleanAirZoneId().equals(cleanAirZone))
        .map(CleanAirZoneDto::getName)
        .findFirst()
        .orElse(null);
  }
}