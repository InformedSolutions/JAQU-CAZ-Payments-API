package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.VehicleEntrantRequest;
import uk.gov.caz.psr.dto.VehicleEntrantResponse;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.service.VehicleEntrantService;

/**
 * A controller which deals with requests that informs about a vehicle entering a CAZ.
 */
@RestController
@AllArgsConstructor
public class VehicleEntrantController implements VehicleEntrantControllerApiSpec {

  @VisibleForTesting
  public static final String BASE_PATH = "/v1/payments";
  @VisibleForTesting
  public static final String CREATE_VEHICLE_ENTRANT_PATH_AND_GET_PAYMENT_DETAILS =
      "vehicle-entrants";

  private final VehicleEntrantService vehicleEntrantService;

  @Override
  public ResponseEntity<VehicleEntrantResponse> createVehicleEntrantAndGetPaymentDetails(
      VehicleEntrantRequest request) {
    InternalPaymentStatus paymentStatus = vehicleEntrantService
        .registerVehicleEntrant(request.toVehicleEntrant());
    return ResponseEntity.ok(VehicleEntrantResponse.from(paymentStatus));
  }
}
