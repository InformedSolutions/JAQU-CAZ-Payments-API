package uk.gov.caz.psr.controller;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.VehicleEntranceRequest;

@RestController
public class PaymentsController implements PaymentsControllerApiSpec {

  static final String BASE_PATH = "/v1/payments";
  static final String CREATE_VEHICLE_ENTRANCE_PATH_AND_GET_PAYMENT_DETAILS = "vehicle-entrants";

  @Override
  public ResponseEntity<Void> createVehicleEntranceAndGetPaymentDetails(
      VehicleEntranceRequest request) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(
      @Valid InitiatePaymentRequest request) {
    InitiatePaymentResponse response = new InitiatePaymentResponse(UUID.randomUUID(),
        "https://payment.next.url");
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
