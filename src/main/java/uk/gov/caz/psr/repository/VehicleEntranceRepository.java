package uk.gov.caz.psr.repository;

import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.VehicleEntrance;

/**
 * A class which handles managing data in {@code VEHICLE_ENTRANCE} table.
 */
@Repository
public class VehicleEntranceRepository {

  /**
   * Inserts {@code vehicleEntrance} into database unless it exists.
   *
   * @param vehicleEntrance An entity object which is supposed to be saved in the database.
   */
  public void insertIfNotExists(VehicleEntrance vehicleEntrance) {
    // TODO to be implemented in CAZ-1238
  }
}
