package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.VehicleEntrance;
import uk.gov.caz.psr.repository.VehicleEntranceRepository;

/**
 * A service which is responsible for keeping the database synchronized with the entrance and
 * payments data.
 */
@Service
@AllArgsConstructor
public class VehicleEntranceService {

  private final VehicleEntranceRepository vehicleEntranceRepository;

  /**
   * Inserts {@code vehicleEntrance} into database (unless it exists) and connects it with the
   * payment information provided it is valid.
   *
   * @param vehicleEntrance A record which is to be inserted into the database.
   * @throws NullPointerException if {@code vehicleEntrance} is {@code null}
   * @throws IllegalArgumentException if {@code vehicleEntrance#id} is {@code null}
   */
  public void registerVehicleEntrance(VehicleEntrance vehicleEntrance) {
    Preconditions.checkNotNull(vehicleEntrance, "Vehicle entrance cannot be null");
    Preconditions.checkArgument(vehicleEntrance.getId() == null,
        "ID of the vehicle entrance must be null, actual: %s", vehicleEntrance.getId());

    vehicleEntranceRepository.insertIfNotExists(vehicleEntrance);

    /*
     * TODO add logic that connects payments that were made in advance if there are any.
     *  Payment must be successfully processed
     */
  }
}
