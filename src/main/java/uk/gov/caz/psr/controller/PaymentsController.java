package uk.gov.caz.psr.controller;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;

@RestController
public class PaymentsController implements PaymentsControllerApiSpec {

  static final String BASE_PATH = "/v1/payments";

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(
      @Valid InitiatePaymentRequest request) {
    InitiatePaymentResponse response = new InitiatePaymentResponse(UUID.randomUUID(),
        "https://payment.next.url");
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
