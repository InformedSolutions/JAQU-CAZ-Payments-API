package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse.ModificationHistoryDetails;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse.VehicleEntrantPaymentDetails;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.util.exception.CleanAirZoneIsNotUniqueException;

/**
 * A utility class that converts {@link Payment} into {@link ReferencesHistoryResponse}.
 */
@Component
@AllArgsConstructor
public class ReferencesHistoryConverter {

  private static final ZoneId GMT_ZONE_ID = ZoneId.of("GMT");
  private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

  private final VccsRepository vccsRepository;

  /**
   * Converts the passed {@link Payment} into an instance of {@link ReferencesHistoryResponse}.
   *
   * @param payment {@link Payment}
   * @return An instance of {@link ReferencesHistoryResponse}.
   */
  public ReferencesHistoryResponse toReferencesHistoryResponse(Payment payment,
      List<PaymentModification> paymentModifications) {
    checkIfCazIsUnique(payment);

    UUID cleanAirZoneId = payment.getEntrantPayments().stream()
        .map(EntrantPayment::getCleanAirZoneId).findFirst().orElse(null);

    return ReferencesHistoryResponse.builder()
        .paymentReference(payment.getReferenceNumber())
        .paymentProviderId(payment.getExternalId())
        .paymentTimestamp(payment.getSubmittedTimestamp())
        .totalPaid(payment.getTotalPaid())
        .telephonePayment(payment.isTelephonePayment())
        .operatorId(payment.getOperatorId())
        .cazName(getCleanAirZoneName(cleanAirZoneId))
        .paymentProviderStatus(payment.getExternalPaymentStatus())
        .lineItems(toVehicleEntrantPaymentsDetails(payment.getEntrantPayments()))
        .modificationHistory(
            ModificationHistoryConverter.toModificationHistory(paymentModifications))
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
        .chargePaid(entrantPayment.getCharge())
        .travelDate(entrantPayment.getTravelDate())
        .vrn(entrantPayment.getVrn())
        .build();
  }

  private ReferencesHistoryResponse.ModificationHistoryDetails toModificationHistoryDetails(
      PaymentModification paymentModification) {
    return ModificationHistoryDetails.builder()
        .amount(paymentModification.getAmount())
        .travelDate(paymentModification.getTravelDate())
        .vrn(paymentModification.getVrn())
        .caseReference(paymentModification.getCaseReference())
        .modificationTimestamp(
            LocalDateTime.from(paymentModification.getModificationTimestamp()).atZone(GMT_ZONE_ID)
                .withZoneSameInstant(UK_ZONE_ID).toLocalDateTime())
        .entrantPaymentStatus(paymentModification.getEntrantPaymentStatus())
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