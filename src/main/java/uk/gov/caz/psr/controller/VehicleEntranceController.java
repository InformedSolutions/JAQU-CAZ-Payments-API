package uk.gov.caz.psr.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.VehicleEntranceRequest;
import uk.gov.caz.psr.service.VehicleEntranceService;

/**
 * A controller which deals with requests that informs about a vehicle entering a CAZ.
 */
@RestController
@AllArgsConstructor
public class VehicleEntranceController implements VehicleEntranceControllerApiSpec {

  static final String BASE_PATH = "/v1/payments";
  static final String CREATE_VEHICLE_ENTRANCE_PATH_AND_GET_PAYMENT_DETAILS = "vehicle-entrants";

  private final VehicleEntranceService vehicleEntranceService;

  @Override
  public void createVehicleEntranceAndGetPaymentDetails(
      VehicleEntranceRequest request) {
    vehicleEntranceService.registerVehicleEntrance(request.toVehicleEntrance());
  }
}
