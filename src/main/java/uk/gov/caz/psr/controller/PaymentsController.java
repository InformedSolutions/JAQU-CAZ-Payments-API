package uk.gov.caz.psr.controller;

import org.springframework.http.ResponseEntity;
import uk.gov.caz.psr.dto.VehicleEntranceRequest;

public class PaymentsController implements PaymentsControllerApiSpec {

  static final String BASE_PATH = "v1/payments";
  static final String CREATE_VEHICLE_ENTRANCE_PATH_AND_GET_PAYMENT_DETAILS = "vehicle-entrants";

  @Override
  public ResponseEntity<Void> createVehicleEntranceAndGetPaymentDetails(
      VehicleEntranceRequest request) {
    throw new UnsupportedOperationException();
  }
}
