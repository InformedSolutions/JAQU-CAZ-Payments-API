package uk.gov.caz.psr.controller;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.GetAndUpdatePaymentStatusResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.GetAndUpdatePaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentsController implements PaymentsControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments";

  private final InitiatePaymentService initiatePaymentService;
  private final GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(InitiatePaymentRequest request) {
    log.info("Received payment request {}", request);
    Payment payment = initiatePaymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(InitiatePaymentResponse.from(payment));
  }

  @Override
  public ResponseEntity<GetAndUpdatePaymentStatusResponse> getExternalPaymentAndUpdateStatus(
      UUID id) {
    Optional<Payment> payment = getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(id);
    return payment.map(GetAndUpdatePaymentStatusResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
