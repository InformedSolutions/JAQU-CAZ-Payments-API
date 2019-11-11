package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.repository.VehicleEntrantRepository;

/**
 * A service which is responsible for keeping the database synchronized with the entrant and
 * payments data.
 */
@Service
@AllArgsConstructor
public class VehicleEntrantService {

  private final VehicleEntrantRepository vehicleEntrantRepository;
  private final FinalizeVehicleEntrantService finalizeVehicleEntrantService;

  /**
   * Inserts {@code vehicleEntrant} into database (unless it exists) and connects it with the
   * payment information provided it is valid.
   *
   * @param vehicleEntrant A record which is to be inserted into the database.
   * @throws NullPointerException     if {@code vehicleEntrant} is {@code null}
   * @throws IllegalArgumentException if {@code vehicleEntrant#id} is {@code null}
   */
  @Transactional
  public void registerVehicleEntrant(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle entrant cannot be null");
    Preconditions.checkArgument(vehicleEntrant.getId() == null,
        "ID of the vehicle entrant must be null, actual: %s", vehicleEntrant.getId());

    vehicleEntrant = vehicleEntrantRepository.insertIfNotExists(vehicleEntrant);
    finalizeVehicleEntrantService.connectExistingVehicleEntrantPayment(vehicleEntrant);
  }
}
