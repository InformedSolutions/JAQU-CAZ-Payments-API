package uk.gov.caz.psr.controller;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.ReconcilePaymentRequest;
import uk.gov.caz.psr.dto.ReconcilePaymentResponse;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.service.ReconcilePaymentStatusService;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentsController implements PaymentsControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments";

  private final InitiatePaymentService initiatePaymentService;
  private final ReconcilePaymentStatusService reconcilePaymentStatusService;

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(InitiatePaymentRequest request) {
    log.info("Received payment request {}", request);
    Payment payment = initiatePaymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(InitiatePaymentResponse.from(payment));
  }

  @Override
  public ResponseEntity<ReconcilePaymentResponse> reconcilePaymentStatus(
      UUID id, ReconcilePaymentRequest request) {
    Optional<Payment> payment = reconcilePaymentStatusService
        .reconcilePaymentStatus(id, request.getCleanAirZoneName());
    return payment.map(ReconcilePaymentResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
