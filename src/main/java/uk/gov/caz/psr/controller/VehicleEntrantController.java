package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.VehicleEntrantRequest;
import uk.gov.caz.psr.dto.VehicleEntrantResponse;
import uk.gov.caz.psr.model.InternalPaymentStatus;

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

  @Override
  public ResponseEntity<VehicleEntrantResponse> createVehicleEntrantAndGetPaymentDetails(
      VehicleEntrantRequest request) {
    // TODO: We are not longer supporting adding VehicleEntrant
    //       will keep it here until the process is fixed.
    //    InternalPaymentStatus paymentStatus = vehicleEntrantService
    //        .registerVehicleEntrant(request.toVehicleEntrant());
    InternalPaymentStatus paymentStatus = InternalPaymentStatus.PAID;
    return ResponseEntity.ok(VehicleEntrantResponse.from(paymentStatus));
  }
}
