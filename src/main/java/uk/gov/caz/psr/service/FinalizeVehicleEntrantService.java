package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;

/**
 * Class responsible for connecting a vehicle entrant record with the successful payment (if
 * exists).
 */
@Service
@AllArgsConstructor
public class FinalizeVehicleEntrantService {

  private final VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  /**
   * Adds {@code VehicleEntrantId} to {@link VehicleEntrantPayment} if {@code Payment} status is
   * SUCCESS.
   *
   * @param vehicleEntrant provided {@link VehicleEntrant} object
   * @return Optional of vehicleEntrantPayment if found for provided VehicleEntrant or
   *     Optional.empty() if vehicleEntrantPayment was not found.
   */
  public Optional<VehicleEntrantPayment> connectExistingVehicleEntrantPayment(
      VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");
    Optional<VehicleEntrantPayment> foundVehicleEntrantPayment = vehicleEntrantPaymentRepository
        .findSuccessfullyPaid(vehicleEntrant);
    foundVehicleEntrantPayment.ifPresent(
        vehicleEntrantPayment -> updateVehicleEntrantPayment(vehicleEntrantPayment,
            vehicleEntrant));
    return foundVehicleEntrantPayment;
  }

  /**
   * Updates found {@link VehicleEntrantPayment} with provided {@link VehicleEntrant} if not
   * connected before.
   *
   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
   * @param vehicleEntrant provided {@link VehicleEntrant} object
   */
  private void updateVehicleEntrantPayment(VehicleEntrantPayment vehicleEntrantPayment,
      VehicleEntrant vehicleEntrant) {
    Preconditions.checkState(vehicleEntrantPayment.getVehicleEntrantId() == null,
        "Payment already assigned to an entrant with id '%s'",
        vehicleEntrantPayment.getVehicleEntrantId());

    vehicleEntrantPaymentRepository.update(rebuildVehicleEntrantPayment(vehicleEntrantPayment,
        vehicleEntrant));
  }

  /**
   * Builds new {@link VehicleEntrantPayment} object with updated {@code vehicleEntrant.id}.
   *
   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
   * @param vehicleEntrant provided {@link VehicleEntrant} object
   */
  private VehicleEntrantPayment rebuildVehicleEntrantPayment(
      VehicleEntrantPayment vehicleEntrantPayment, VehicleEntrant vehicleEntrant) {
    return vehicleEntrantPayment.toBuilder()
        .vehicleEntrantId(vehicleEntrant.getId())
        .build();
  }
}