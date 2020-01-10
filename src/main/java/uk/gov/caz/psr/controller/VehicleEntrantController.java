package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.CazEntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;

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
  public ResponseEntity<List<CazEntrantPaymentDto>> createVehicleEntrantAndGetPaymentDetails(
      List<VehicleEntrantDto> vehicleEntrants) {
    // TODO: We are not longer supporting adding VehicleEntrant
    //       will keep it here until the process is fixed.
    //    InternalPaymentStatus paymentStatus = vehicleEntrantService
    //        .registerVehicleEntrant(request.toVehicleEntrant());

    CazEntrantPaymentDto dto = CazEntrantPaymentDto
        .builder()
        .paymentStatus("paid")
        .vehicleEntrantId(UUID.fromString("0e17dbe3-3e62-476e-977f-fa3c1c12e9b7"))
        .build();

    return ResponseEntity.ok(Arrays.asList(dto));
  }
}
