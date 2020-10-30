package uk.gov.caz.psr.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.dto.EntrantPaymentWithLatestPaymentDetailsDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

@Service
@AllArgsConstructor
public class EntrantPaymentService {

  private static final ZoneId GMT_ZONE_ID = ZoneId.of("GMT");
  private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

  private final EntrantPaymentRepository entrantPaymentRepository;
  private final PaymentRepository paymentRepository;

  /**
   * Method receives a collection of (cazId, cazEntryTimestamp, vrn) and accordingly creates or
   * updates data in {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT}.
   *
   * @param vehicleEntrants list of objects.
   * @return List of {@link EntrantPaymentWithLatestPaymentDetailsDto}.
   */
  @Transactional
  public List<EntrantPaymentWithLatestPaymentDetailsDto> bulkProcess(
      List<VehicleEntrantDto> vehicleEntrants) {
    List<EntrantPaymentWithLatestPaymentDetailsDto> result = new ArrayList<>();

    for (VehicleEntrantDto vehicleEntrantDto : vehicleEntrants) {
      Optional<EntrantPayment> entrantPayment = fetchEntrantPaymentFromRepository(
          vehicleEntrantDto);

      if (thereIsNoExisting(entrantPayment)) {
        processNewEntrant(result, vehicleEntrantDto);
      } else {
        processExistingEntrant(result, entrantPayment.orElse(null)); // <- orElse is for Sonar
      }
    }

    return result;
  }

  /**
   * Tries to fetch {@link EntrantPayment} from DB on this CAZ, day and VRN. It is possible that
   * there is no entry yet and then it returns Optional.empty().
   */
  private Optional<EntrantPayment> fetchEntrantPaymentFromRepository(
      VehicleEntrantDto vehicleEntrantDto) {

    return entrantPaymentRepository.findOneByVrnAndCazEntryDate(
        vehicleEntrantDto.getCleanZoneId(),
        vehicleEntrantDto.getVrn(),
        LocalDateTime.from(vehicleEntrantDto.getCazEntryTimestamp().atZone(GMT_ZONE_ID)
            .withZoneSameInstant(UK_ZONE_ID)).toLocalDate()
    ).map(entrantPayment -> entrantPayment.toBuilder()
        .cazEntryTimestamp(vehicleEntrantDto.getCazEntryTimestamp())
        .build()
    );
  }

  /**
   * Returns true if {@link EntrantPayment} is not present for this day and CAZ. It has not been
   * paid in advance and this is first entrance on this day.
   */
  private boolean thereIsNoExisting(Optional<EntrantPayment> record) {
    return !record.isPresent();
  }

  /**
   * If {@link EntrantPayment} is not present at all it means it was not paid in advance or this is
   * first vehicle entry in this CAZ on this day. In this case we are sure that there are no
   * payments so we can quickly add new {@link EntrantPayment} to the DB and return DTO without any
   * payment details.
   */
  private void processNewEntrant(List<EntrantPaymentWithLatestPaymentDetailsDto> result,
      VehicleEntrantDto vehicleEntrantDto) {
    EntrantPayment entrantPayment = addNewEntrantPayment(vehicleEntrantDto);
    addToResultWithoutPaymentDetails(entrantPayment, result);
  }

  /**
   * If {@link EntrantPayment} is present it means that it was either paid in advance (or at least
   * it was tried to be paid) or this is not-the-first vehicle entry in this CAZ on this day. In
   * this case we need to check if vehicle entrance hasn't been already captured (if this is first
   * entry but paid in advance case) and if no, mark it as captured. Following that we need to check
   * if it has been "PAID" and if yes try to fetch corresponding "latest" {@link Payment} entity.
   * Finally if everything is present and only then we can fetch PaymentMethod (if entrant is
   * "PAID"). Else we return "null" as PaymentMethod.
   */
  private void processExistingEntrant(List<EntrantPaymentWithLatestPaymentDetailsDto> result,
      EntrantPayment entrantPayment) {
    markAsVehicleEntrantCapturedIfNotAlreadySo(entrantPayment);
    if (isPaid(entrantPayment)) {
      processWithPotentialPayment(result, entrantPayment);
    } else {
      addToResultWithoutPaymentDetails(entrantPayment, result);
    }
  }

  /**
   * Forges new {@link EntrantPayment} entity for this CAZ and day and saves to the DB.
   */
  private EntrantPayment addNewEntrantPayment(VehicleEntrantDto vehicleEntrantDto) {
    EntrantPayment entrantPayment = buildNewEntrantPayment(vehicleEntrantDto);
    return entrantPaymentRepository.insert(entrantPayment);
  }

  /**
   * Creates new {@link EntrantPayment} from incoming DTO. Status will be "NOT_PAID", it will be
   * "captured" because we know this request comes from ANPR through VCCS.
   */
  private EntrantPayment buildNewEntrantPayment(VehicleEntrantDto vehicleEntrantDto) {
    return EntrantPayment.builder()
        .vrn(vehicleEntrantDto.getVrn())
        .cleanAirZoneId(vehicleEntrantDto.getCleanZoneId())
        .travelDate(LocalDateTime.from(vehicleEntrantDto.getCazEntryTimestamp().atZone(GMT_ZONE_ID)
            .withZoneSameInstant(UK_ZONE_ID)).toLocalDate())
        .cazEntryTimestamp(vehicleEntrantDto.getCazEntryTimestamp())
        .vehicleEntrantCaptured(true)
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .build();
  }

  /**
   * Maps {@link EntrantPayment} to {@link EntrantPaymentWithLatestPaymentDetailsDto} and puts into
   * resulting list. Skips all {@link Payment} properties because there is no Payment or {@link
   * EntrantPayment} is not "PAID". Payment properties in DTO will be "null".
   */
  private void addToResultWithoutPaymentDetails(
      EntrantPayment entrantPayment, List<EntrantPaymentWithLatestPaymentDetailsDto> result) {
    result
        .add(EntrantPaymentWithLatestPaymentDetailsDto.fromEntrantPaymentOnly(entrantPayment));
  }

  /**
   * If "vehicleEntrantCaptured" flag is not already true on {@link EntrantPayment} it will be
   * updated to "true" meaning that we indeed had vehicle entrance on this day in this CAZ.
   */
  private void markAsVehicleEntrantCapturedIfNotAlreadySo(EntrantPayment entrantPayment) {
    if (!entrantPayment.isVehicleEntrantCaptured()) {
      EntrantPayment entrantPaymentToUpdate = entrantPayment.toBuilder()
          .vehicleEntrantCaptured(true).build();
      entrantPaymentRepository.update(entrantPaymentToUpdate);
    }
  }

  /**
   * Returns true if {@link EntrantPayment} has been paid. Only for such entrants we may return
   * PaymentMethod if {@link Payment} record is present.
   */
  private boolean isPaid(EntrantPayment entrantPayment) {
    return entrantPayment.getInternalPaymentStatus() == InternalPaymentStatus.PAID;
  }

  /**
   * If {@link EntrantPayment} is present and is "PAID" we try to fetch corresponding matching
   * latest {@link Payment} record. If it is present we can fetch PaymentMethod to return. If it is
   * not present we will return "null" as PaymentMethod.
   */
  private void processWithPotentialPayment(List<EntrantPaymentWithLatestPaymentDetailsDto> result,
      EntrantPayment entrantPayment) {
    Optional<Payment> payment = findLatestPaymentIfPresent(entrantPayment);
    if (payment.isPresent()) {
      addToResultWithPaymentDetails(entrantPayment, payment.get(), result);
    } else {
      addToResultWithoutPaymentDetails(entrantPayment, result);
    }
  }

  /**
   * Tries to find corresponding "latest" {@link Payment} record for {@link EntrantPayment}. It is
   * possible that there is no Payment yet and the it returns Optional.empty().
   */
  private Optional<Payment> findLatestPaymentIfPresent(EntrantPayment entrantPayment) {
    return paymentRepository
        .findByEntrantPayment(entrantPayment.getCleanAirZoneEntrantPaymentId());
  }

  /**
   * Maps {@link EntrantPayment} and {@link Payment}
   * to {@link EntrantPaymentWithLatestPaymentDetailsDto} and puts into resulting list.
   */
  private void addToResultWithPaymentDetails(
      EntrantPayment entrantPayment, Payment payment,
      List<EntrantPaymentWithLatestPaymentDetailsDto> result) {
    result.add(
        EntrantPaymentWithLatestPaymentDetailsDto.from(entrantPayment, payment));
  }
}
