package uk.gov.caz.psr.controller;

import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
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
public class PaymentsController implements PaymentsControllerApiSpec {

  static final String BASE_PATH = "/v1/payments";

  private final InitiatePaymentService initiatePaymentService;
  private final GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(
      @Valid InitiatePaymentRequest request, String correlationId) {

    Payment payment = initiatePaymentService.createPayment(request, correlationId);

    InitiatePaymentResponse response = new InitiatePaymentResponse(payment.getId(),
        payment.getNextUrl());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
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
