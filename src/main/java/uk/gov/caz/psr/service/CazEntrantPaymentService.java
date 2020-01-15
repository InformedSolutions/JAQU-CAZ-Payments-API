package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.CazEntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.repository.CazEntrantPaymentRepository;

@Service
@AllArgsConstructor
@SuppressWarnings("PMD.EmptyIfStmt") // TODO: Remove in CAZ-1715
public class CazEntrantPaymentService {

  private final CazEntrantPaymentRepository cazEntrantPaymentRepository;

  /**
   * Method receives a collection of (cazId, cazEntryTimestamp, vrn) and accordingly creates or
   * updates data in {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT}.
   *
   * @param vehicleEntrants list of objects.
   * @return List of {@link CazEntrantPaymentDto}.
   */
  public List<CazEntrantPaymentDto> bulkProcess(List<VehicleEntrantDto> vehicleEntrants) {
    List<CazEntrantPaymentDto> cazEntrantPayments = new ArrayList<>();

    for (VehicleEntrantDto vehicleEntrantDto : vehicleEntrants) {
      Optional<CazEntrantPayment> record = cazEntrantPaymentRepository
          .findOneByVrnAndCazEntryDate(
              vehicleEntrantDto.getCleanZoneId(),
              vehicleEntrantDto.getVrn(),
              vehicleEntrantDto.getCazEntryTimestamp().toLocalDate()
          );

      if (record.isPresent()) {
        CazEntrantPayment cazEntrantPayment = record.get();
        CazEntrantPaymentDto cazEntrantPaymentDto = CazEntrantPaymentDto.from(cazEntrantPayment);
        cazEntrantPayments.add(cazEntrantPaymentDto);

        if (!cazEntrantPayment.isVehicleEntrantCaptured()) {
          CazEntrantPayment cazEntrantPaymentToUpdate = cazEntrantPayment.toBuilder()
              .vehicleEntrantCaptured(true).build();
          cazEntrantPaymentRepository.update(cazEntrantPaymentToUpdate);
        }
      } else {
        // TODO: CAZ-1715
      }
    }
    return cazEntrantPayments;
  }
}
