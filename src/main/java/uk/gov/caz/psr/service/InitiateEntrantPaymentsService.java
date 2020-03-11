package uk.gov.caz.psr.service;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentMatch;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.SingleEntrantPayment;
import uk.gov.caz.psr.repository.EntrantPaymentMatchRepository;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Processes {@link EntrantPayment}s when a new payment is initiated.
 */
@Slf4j
@AllArgsConstructor
@Service
public class InitiateEntrantPaymentsService {

  private final EntrantPaymentRepository entrantPaymentRepository;
  private final EntrantPaymentMatchRepository entrantPaymentMatchRepository;
  private final PaymentRepository paymentRepository;
  private final CleanupDanglingPaymentService cleanupDanglingPaymentService;

  /**
   * Processes {@link EntrantPayment}s when a new payment is initiated by inserting or updating
   * {@link EntrantPayment} entities alongside with inserting or updating {@link
   * EntrantPaymentMatch}es.
   */
  void processEntrantPaymentsForPayment(UUID paymentId, UUID cleanAirZoneId,
      List<SingleEntrantPayment> entrantPayments) {

    Map<String, List<SingleEntrantPayment>> groupedByVrn = entrantPayments.stream()
        .collect(Collectors.groupingBy(SingleEntrantPayment::getVrn));

    for (Entry<String, List<SingleEntrantPayment>> vrnWithPayment : groupedByVrn.entrySet()) {
      processEntrantPaymentsForVrn(paymentId, cleanAirZoneId, vrnWithPayment.getKey(),
          vrnWithPayment.getValue());
    }
  }

  /**
   * Process all 'transactions' (in a data sense, see {@code InitiatePaymentRequest}) related to a
   * particular {@code vrn}.
   */
  private void processEntrantPaymentsForVrn(UUID paymentId, UUID cleanAirZoneId, String vrn,
      List<SingleEntrantPayment> entrantPayments) {
    List<EntrantPayment> currentEntrantPayments = fetchMatchingEntrantPayments(
        cleanAirZoneId, vrn, entrantPayments);
    for (SingleEntrantPayment entrantPayment : entrantPayments) {
      processEntrantPayment(paymentId, cleanAirZoneId, vrn, currentEntrantPayments, entrantPayment);
    }
  }

  /**
   * Finds matching entrant payments for {@code cleanAirZoneId} and {@code vrn} in {@code
   * entrantPayments}.
   */
  private List<EntrantPayment> fetchMatchingEntrantPayments(UUID cleanAirZoneId, String vrn,
      List<SingleEntrantPayment> entrantPayments) {
    List<LocalDate> travelDates = entrantPayments.stream().map(SingleEntrantPayment::getTravelDate)
        .collect(Collectors.toList());
    return entrantPaymentRepository.findByVrnAndCazEntryDates(cleanAirZoneId, vrn, travelDates);
  }

  /**
   * Process a single 'transaction' (in a data sense, see {@code InitiatePaymentRequest}).
   */
  private void processEntrantPayment(UUID paymentId, UUID cleanAirZoneId, String vrn,
      List<EntrantPayment> currentEntrantPayments, SingleEntrantPayment singleEntrantPayment) {
    Optional<EntrantPayment> entrantPayment = findMatchingEntrantPayment(
        singleEntrantPayment.getTravelDate(), currentEntrantPayments);
    UUID cleanAirZoneEntrantPaymentId;
    if (entrantPayment.isPresent()) {
      processRelatedPayment(entrantPayment.get());
      cleanAirZoneEntrantPaymentId = updateEntrantPayment(singleEntrantPayment.getTariffCode(),
          singleEntrantPayment.getCharge(), entrantPayment.get());
      entrantPaymentMatchRepository.updateLatestToFalseFor(cleanAirZoneEntrantPaymentId);
    } else {
      cleanAirZoneEntrantPaymentId = insertEntrantPayment(cleanAirZoneId, vrn,
          singleEntrantPayment.getTariffCode(), singleEntrantPayment.getCharge(),
          singleEntrantPayment.getTravelDate());
    }
    matchPaymentWithEntrantPayment(paymentId, cleanAirZoneEntrantPaymentId);
  }

  /**
   * Checks whether the associated payment for {@code entrantPayment} is finished or the payment has
   * already been successfully completed.
   * Checks whether the associated payment for {@code entrantPayment} is finished or the payment has
   * already been successfully completed. If the payment is not finished yet, the payment is
   * considered a dangling one and the same logic as for dangling payments is applied.
   */
  private void processRelatedPayment(EntrantPayment entrantPayment) {
    if (InternalPaymentStatus.PAID == entrantPayment.getInternalPaymentStatus()) {
      throw new IllegalStateException("Cannot process the payment as the entrant on "
          + entrantPayment.getTravelDate() + " has already been paid");
    }

    ExternalPaymentStatus relatedPaymentStatus = paymentRepository
        .findByEntrantPayment(entrantPayment.getCleanAirZoneEntrantPaymentId())
        .filter(payment -> {
          boolean isDanglingPayment = payment.getExternalPaymentStatus().isNotFinished();
          log.info("The related payment for entrant '{}' is {}",
              entrantPayment.getCleanAirZoneEntrantPaymentId(),
              isDanglingPayment ? "a dangling one" : "not a dangling one");
          return isDanglingPayment;
        })
        .map(cleanupDanglingPaymentService::processDanglingPayment)
        .map(Payment::getExternalPaymentStatus)
        .orElse(null);

    if (relatedPaymentStatus != null && (relatedPaymentStatus.isNotFinished()
        || relatedPaymentStatus == ExternalPaymentStatus.SUCCESS)) {
      throw new IllegalStateException("The corresponding payment has been completed or not "
          + "finished yet, its state is equal to " + relatedPaymentStatus);
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
  private UUID insertEntrantPayment(UUID cleanAirZoneId, String vrn, String tariffCode,
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
  private Optional<EntrantPayment> findMatchingEntrantPayment(LocalDate travelDate,
      List<EntrantPayment> currentEntrantPayments) {
    return currentEntrantPayments.stream()
        .filter(entrantPayment -> travelDate.equals(entrantPayment.getTravelDate()))
        .findFirst();
  }
}
