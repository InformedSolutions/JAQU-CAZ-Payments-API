package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.PaidPaymentsRequest;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.dto.ReconcilePaymentResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.GetPaidEntrantPaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.service.ReconcilePaymentStatusService;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentsController implements PaymentsControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments";

  @VisibleForTesting
  public static final String GET_PAID_VEHICLE_ENTRANTS = "paid";

  private final InitiatePaymentService initiatePaymentService;
  private final ReconcilePaymentStatusService reconcilePaymentStatusService;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(InitiatePaymentRequest request) {
    log.info("Received payment request {}", request);
    Payment payment = initiatePaymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(InitiatePaymentResponse.from(payment));
  }

  @Override
  public ResponseEntity<ReconcilePaymentResponse> reconcilePaymentStatus(UUID id) {
    Optional<Payment> payment = reconcilePaymentStatusService
        .reconcilePaymentStatus(id);
    return payment.map(ReconcilePaymentResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<PaidPaymentsResponse> getPaidEntrantPayment(
      PaidPaymentsRequest paymentsRequest) {
    paymentsRequest.validate();

    Map<String, List<EntrantPayment>> results = getPaidEntrantPaymentsService.getResults(
        new HashSet<String>(paymentsRequest.getVrns()),
        paymentsRequest.getStartDate(),
        paymentsRequest.getEndDate(),
        paymentsRequest.getCleanAirZoneId()
    );

    return ResponseEntity.ok(PaidPaymentsResponse.from(results));
  }
}
