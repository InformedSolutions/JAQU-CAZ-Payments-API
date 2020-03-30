package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.EntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;

@Service
@AllArgsConstructor
public class EntrantPaymentService {

  private final EntrantPaymentRepository entrantPaymentRepository;

  /**
   * Method receives a collection of (cazId, cazEntryTimestamp, vrn) and accordingly creates or
   * updates data in {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT}.
   *
   * @param vehicleEntrants list of objects.
   * @return List of {@link EntrantPaymentDto}.
   */
  public List<EntrantPaymentDto> bulkProcess(List<VehicleEntrantDto> vehicleEntrants) {
    List<EntrantPaymentDto> result = new ArrayList<>();

    for (VehicleEntrantDto vehicleEntrantDto : vehicleEntrants) {
      Optional<EntrantPayment> record = fetchEntrantPaymentFromRepository(vehicleEntrantDto);

      EntrantPayment entrantPayment = record.isPresent()
          ? record.get()
          : storeNewEntrantPayment(vehicleEntrantDto);

      updateVehicleEntrantCapturedIfNotCaptured(entrantPayment);
      result.add(EntrantPaymentDto.from(entrantPayment));
    }

    return result;
  }

  private Optional<EntrantPayment> fetchEntrantPaymentFromRepository(
      VehicleEntrantDto vehicleEntrantDto) {
    return entrantPaymentRepository
        .findOneByVrnAndCazEntryDate(
            vehicleEntrantDto.getCleanZoneId(),
            vehicleEntrantDto.getVrn(),
            vehicleEntrantDto.getCazEntryTimestamp().toLocalDate()
        );
  }

  private void updateVehicleEntrantCapturedIfNotCaptured(EntrantPayment entrantPayment) {
    if (!entrantPayment.isVehicleEntrantCaptured()) {
      EntrantPayment entrantPaymentToUpdate = entrantPayment.toBuilder()
          .vehicleEntrantCaptured(true).build();
      entrantPaymentRepository.update(entrantPaymentToUpdate);
    }
  }

  private EntrantPayment storeNewEntrantPayment(VehicleEntrantDto vehicleEntrantDto) {
    EntrantPayment entrantPayment = buildNewEntrantPayment(vehicleEntrantDto);
    return entrantPaymentRepository.insert(entrantPayment);
  }

  private EntrantPayment buildNewEntrantPayment(VehicleEntrantDto vehicleEntrantDto) {
    return EntrantPayment.builder()
        .vrn(vehicleEntrantDto.getVrn())
        .cleanAirZoneId(vehicleEntrantDto.getCleanZoneId())
        .travelDate(vehicleEntrantDto.getCazEntryTimestamp().toLocalDate())
        .vehicleEntrantCaptured(true)
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .build();
  }
}
