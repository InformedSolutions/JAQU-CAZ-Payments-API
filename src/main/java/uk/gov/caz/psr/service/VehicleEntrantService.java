package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;

/**
 * A service which is responsible for keeping the database synchronized with the entrant and
 * payments data.
 */
@Service
@AllArgsConstructor
public class VehicleEntrantService {

  /**
   * Inserts {@code vehicleEntrant} into database (unless it exists) and connects it with the
   * payment information provided it is valid.
   *
   * @param vehicleEntrant A record which is to be inserted into the database.
   * @throws NullPointerException if {@code vehicleEntrant} is {@code null}
   * @throws IllegalArgumentException if {@code vehicleEntrant#id} is {@code null}
   * @return InternalPaymentStatus for created Vehicle Entrant
   */
  @Transactional
  public InternalPaymentStatus registerVehicleEntrant(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle entrant cannot be null");
    Preconditions.checkArgument(vehicleEntrant.getId() == null,
        "ID of the vehicle entrant must be null, actual: %s", vehicleEntrant.getId());

    // TODO fix finalization
    return InternalPaymentStatus.NOT_PAID;
  }

}
