package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.EntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;

@Service
@AllArgsConstructor
@SuppressWarnings("PMD.EmptyIfStmt") // TODO: Remove in CAZ-1715
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
    List<EntrantPaymentDto> cazEntrantPayments = new ArrayList<>();

    for (VehicleEntrantDto vehicleEntrantDto : vehicleEntrants) {
      Optional<EntrantPayment> record = entrantPaymentRepository
          .findOneByVrnAndCazEntryDate(
              vehicleEntrantDto.getCleanZoneId(),
              vehicleEntrantDto.getVrn(),
              vehicleEntrantDto.getCazEntryTimestamp().toLocalDate()
          );

      if (record.isPresent()) {
        EntrantPayment cazEntrantPayment = record.get();
        EntrantPaymentDto cazEntrantPaymentDto = EntrantPaymentDto.from(cazEntrantPayment);
        cazEntrantPayments.add(cazEntrantPaymentDto);

        if (!cazEntrantPayment.isVehicleEntrantCaptured()) {
          EntrantPayment cazEntrantPaymentToUpdate = cazEntrantPayment.toBuilder()
              .vehicleEntrantCaptured(true).build();
          entrantPaymentRepository.update(cazEntrantPaymentToUpdate);
        }
      } else {
        // TODO: CAZ-1715
      }
    }
    return cazEntrantPayments;
  }
}
