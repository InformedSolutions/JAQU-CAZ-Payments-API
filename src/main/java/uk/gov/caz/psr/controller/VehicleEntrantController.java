package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.EntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.service.EntrantPaymentService;

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

  private final EntrantPaymentService entrantPaymentService;

  @Override
  public ResponseEntity<List<EntrantPaymentDto>> createVehicleEntrantAndGetPaymentDetails(
      List<VehicleEntrantDto> vehicleEntrants) {

    List<EntrantPaymentDto> cazEntrantPaymentDtos = entrantPaymentService
        .bulkProcess(vehicleEntrants);

    return ResponseEntity.ok(cazEntrantPaymentDtos);
  }
}
