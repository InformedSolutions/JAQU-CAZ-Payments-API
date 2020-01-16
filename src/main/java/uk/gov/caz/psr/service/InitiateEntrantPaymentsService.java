package uk.gov.caz.psr.service;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentMatch;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.EntrantPaymentMatchRepository;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;

/**
 * Processes {@link EntrantPayment}s when a new payment is initiated.
 */
@AllArgsConstructor
@Service
public class InitiateEntrantPaymentsService {

  private final EntrantPaymentRepository entrantPaymentRepository;
  private final EntrantPaymentMatchRepository entrantPaymentMatchRepository;
  private final VehicleEntrantPaymentChargeCalculator chargeCalculator;

  /**
   * Processes {@link EntrantPayment}s when a new payment is initiated by inserting or updating
   * {@link EntrantPayment} entities alongside with inserting or updating {@link
   * EntrantPaymentMatch}es.
   */
  void processEntrantPaymentsForPayment(UUID paymentId, int total, List<LocalDate> travelDates,
      String tariffCode, String vrn, UUID cleanAirZoneId) {
    int chargePerDay = chargeCalculator.calculateCharge(total, travelDates.size());
    List<EntrantPayment> currentEntrantPayments = entrantPaymentRepository
        .findByVrnAndCazEntryDates(cleanAirZoneId, vrn, travelDates);

    for (LocalDate travelDate : travelDates) {
      Optional<EntrantPayment> entrantPayment = findEntrantPaymentForTravelDate(travelDate,
          currentEntrantPayments);
      UUID cleanAirZoneEntrantPaymentId;
      if (entrantPayment.isPresent()) {
        // TODO CAZ-1719 check if entrant payment is paid or pending
        cleanAirZoneEntrantPaymentId = updateEntrantPayment(tariffCode, chargePerDay,
            entrantPayment.get());
        entrantPaymentMatchRepository.updateLatestToFalseFor(cleanAirZoneEntrantPaymentId);
      } else {
        cleanAirZoneEntrantPaymentId = insertEntrantPayment(tariffCode, vrn, cleanAirZoneId,
            chargePerDay, travelDate);
      }
      matchPaymentWithEntrantPayment(paymentId, cleanAirZoneEntrantPaymentId);
    }
  }

  /**
   * Inserts a new {@link EntrantPaymentMatch} records that match a payment with entrants.
   */
  private void matchPaymentWithEntrantPayment(UUID paymentId, UUID cleanAirZoneEntrantPaymentId) {
    EntrantPaymentMatch entrantPaymentMatch = EntrantPaymentMatch.builder()
        .paymentId(paymentId)
        .vehicleEntrantPaymentId(cleanAirZoneEntrantPaymentId)
        .latest(true)
        .build();
    entrantPaymentMatchRepository.insert(entrantPaymentMatch);
  }

  /**
   * Inserts a new instance of {@link EntrantPayment} based on the passed arguments and returns its
   * identifier.
   */
  private UUID insertEntrantPayment(String tariffCode, String vrn, UUID cleanAirZoneId,
      int chargePerDay, LocalDate travelDate) {
    EntrantPayment toBeInserted = buildEntrantPayment(travelDate, chargePerDay,
        vrn, tariffCode, cleanAirZoneId);
    EntrantPayment inserted = entrantPaymentRepository.insert(toBeInserted);
    return inserted.getCleanAirZoneEntrantPaymentId();
  }

  /**
   * Updates an existing {@link EntrantPayment} by setting charge, tariff code and the update actor.
   * Once the update has been made, the identifier of the entity is returned.
   */
  private UUID updateEntrantPayment(String tariffCode, int chargePerDay,
      EntrantPayment entrantPayment) {
    EntrantPayment toBeUpdated = entrantPayment.toBuilder()
        .updateActor(EntrantPaymentUpdateActor.USER)
        .charge(chargePerDay)
        .tariffCode(tariffCode)
        .build();
    entrantPaymentRepository.update(toBeUpdated);
    return toBeUpdated.getCleanAirZoneEntrantPaymentId();
  }

  /**
   * Creates an instance of {@link EntrantPayment}.
   */
  private EntrantPayment buildEntrantPayment(LocalDate travelDate, int chargePerDay, String vrn,
      String tariffCode, UUID cleanAirZoneId) {
    return EntrantPayment.builder()
        .vrn(normalizeVrn(vrn))
        .cleanAirZoneId(cleanAirZoneId)
        .travelDate(travelDate)
        .charge(chargePerDay)
        .updateActor(EntrantPaymentUpdateActor.USER)
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .tariffCode(tariffCode)
        .build();
  }

  /**
   * Finds an entrant payment in the passed {@code currentEntrantPayments} whose travel date is
   * equal to {@code travelDate}.
   */
  private Optional<EntrantPayment> findEntrantPaymentForTravelDate(LocalDate travelDate,
      List<EntrantPayment> currentEntrantPayments) {
    return currentEntrantPayments.stream()
        .filter(entrantPayment -> travelDate.equals(entrantPayment.getTravelDate()))
        .findFirst();
  }
}
