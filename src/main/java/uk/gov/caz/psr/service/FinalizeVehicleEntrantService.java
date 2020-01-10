package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.repository.CazEntrantPaymentRepository;

/**
 * Class responsible for connecting a vehicle entrant record with the successful payment (if
 * exists).
 */
@Service
@AllArgsConstructor
public class FinalizeVehicleEntrantService {

  private final CazEntrantPaymentRepository cazEntrantPaymentRepository;

  /**
   * Adds {@code VehicleEntrantId} to {@link CazEntrantPayment} if {@code Payment} status is
   * SUCCESS.
   *
   * @param vehicleEntrant provided {@link VehicleEntrant} object
   * @return Optional of vehicleEntrantPayment if found for provided VehicleEntrant or
   *     Optional.empty() if vehicleEntrantPayment was not found.
   */
  public Optional<CazEntrantPayment> connectExistingVehicleEntrantPayment(
      VehicleEntrant vehicleEntrant) {

    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");
    //    TODO: We are not longer supporting adding VehicleEntrant will keep it here
    //          until the process is fixed.
    //    Optional<VehicleEntrantPayment> foundVehicleEntrantPayment =
    //      vehicleEntrantPaymentRepository
    //        .findSuccessfullyPaid(vehicleEntrant);
    //    foundVehicleEntrantPayment.ifPresent(
    //        vehicleEntrantPayment -> updateVehicleEntrantPayment(vehicleEntrantPayment,
    //            vehicleEntrant));
    //    return foundVehicleEn trantPayment;
    return  Optional.empty();
  }

  //  /**
  //   * Updates found {@link VehicleEntrantPayment} with provided {@link VehicleEntrant} if not
  //   * connected before.
  //   *
  //   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
  //   * @param vehicleEntrant provided {@link VehicleEntrant} object
  //   */
  //  private void updateVehicleEntrantPayment(VehicleEntrantPayment vehicleEntrantPayment,
  //      VehicleEntrant vehicleEntrant) {
  //    Preconditions.checkState(vehicleEntrantPayment.getVehicleEntrantId() == null,
  //        "Payment already assigned to an entrant with id '%s'",
  //        vehicleEntrantPayment.getVehicleEntrantId());
  //
  //    vehicleEntrantPaymentRepository.update(rebuildVehicleEntrantPayment(vehicleEntrantPayment,
  //        vehicleEntrant));
  //  }
  //
  //  /**
  //   * Builds new {@link VehicleEntrantPayment} object with updated {@code vehicleEntrant.id}.
  //   *
  //   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
  //   * @param vehicleEntrant provided {@link VehicleEntrant} object
  //   */
  //  private VehicleEntrantPayment rebuildVehicleEntrantPayment(
  //      VehicleEntrantPayment vehicleEntrantPayment, VehicleEntrant vehicleEntrant) {
  //    return vehicleEntrantPayment.toBuilder()
  //        .vehicleEntrantId(vehicleEntrant.getId())
  //        .build();
  //  }
}