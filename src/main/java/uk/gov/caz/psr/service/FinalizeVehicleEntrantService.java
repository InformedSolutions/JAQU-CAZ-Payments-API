package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;

@Service
@AllArgsConstructor
public class FinalizeVehicleEntrantService {

  private final VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  /**
   * Adds {@code VehicleEntrantId} to {@link VehicleEntrantPayment} if {@code Payment} status is
   * SUCCESS.
   *
   * @param vehicleEntrant provided {@link VehicleEntrant} object
   */
  public void connectExistingVehicleEntrantPayment(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");
    Optional<VehicleEntrantPayment> foundVehicleEntrantPayment = vehicleEntrantPaymentRepository
        .findSuccessfullyPaid(vehicleEntrant);
    foundVehicleEntrantPayment.ifPresent(
        vehicleEntrantPayment -> updateVehicleEntrantPayment(vehicleEntrantPayment,
            vehicleEntrant));
  }

  /**
   * Updates found {@link VehicleEntrantPayment} with provided {@link VehicleEntrant} if not
   * connected before.
   *
   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
   * @param vehicleEntrant        provided {@link VehicleEntrant} object
   */
  private void updateVehicleEntrantPayment(VehicleEntrantPayment vehicleEntrantPayment,
      VehicleEntrant vehicleEntrant) {
    if (vehicleEntrantPayment.getVehicleEntrantId() != null) {
      throw new IllegalStateException("Payment already assigned to Entrant.");
    }

    vehicleEntrantPaymentRepository
        .update(rebuildVehicleEntrantPayment(vehicleEntrantPayment, vehicleEntrant));
  }

  /**
   * Builds new {@link VehicleEntrantPayment} object with updated {@code vehicleEntrant.id}.
   *
   * @param vehicleEntrantPayment found {@link VehicleEntrantPayment} object.
   * @param vehicleEntrant        provided {@link VehicleEntrant} object
   */
  private VehicleEntrantPayment rebuildVehicleEntrantPayment(
      VehicleEntrantPayment vehicleEntrantPayment, VehicleEntrant vehicleEntrant) {
    return vehicleEntrantPayment.toBuilder()
        .vehicleEntrantId(vehicleEntrant.getId())
        .build();
  }
}