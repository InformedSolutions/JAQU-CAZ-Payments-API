package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.CazEntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.service.CazEntrantPaymentService;

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

  private final CazEntrantPaymentService cazEntrantPaymentService;

  @Override
  public ResponseEntity<List<CazEntrantPaymentDto>> createVehicleEntrantAndGetPaymentDetails(
      List<VehicleEntrantDto> vehicleEntrants) {

    List<CazEntrantPaymentDto> cazEntrantPaymentDtos = cazEntrantPaymentService
        .bulkProcess(vehicleEntrants);

    return ResponseEntity.ok(cazEntrantPaymentDtos);
  }
}
