package uk.gov.caz.psr.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * A class which holds attributes related for chargeable vehicles list with pagination.
 */
@Value
@Builder(toBuilder = true)
public class ChargeableVehiclesPage {

  /**
   * The tariff code for the vehicle in a given Clean Air Zone.
   */
  List<ChargeableVehicle> chargeableVehicles;

  /**
   * Total number of account chargeable vehicles.
   */
  long totalVehiclesCount;

  /**
   * Total number of pages.
   */
  int pageCount;
}
