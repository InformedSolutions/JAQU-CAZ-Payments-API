package uk.gov.caz.psr.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentStatusRequest;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.dto.PaymentUpdateSuccessResponse;
import uk.gov.caz.psr.service.PaymentStatusUpdateService;

/**
 * A controller which exposes endpoints dealing with charge settlement.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class ChargeSettlementController implements ChargeSettlementControllerApiSpec {

  public static final String BASE_PATH = "/v1/charge-settlement";
  public static final String PAYMENT_INFO_PATH = "/payment-info";
  public static final String PAYMENT_STATUS_PATH = "/payment-status";

  private PaymentStatusUpdateService paymentStatusUpdateService;

  @Override
  public ResponseEntity<PaymentInfoResponse> getPaymentInfo(PaymentInfoRequest paymentInfoRequest) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus(PaymentStatusRequest request) {
    /* TODO: Fetch:
     *    paymentStatus from VEHICLE_ENTRANT_PAYMENT table
     *    paymentId from PAYMENT table
     *    paymentStatus from VEHICLE_ENTRANT_PAYMENT table
     *
     **/
    log.info("Received request: {}", request);

    PaymentStatusResponse fakeResponse = PaymentStatusResponse.builder()
        .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.PAID)
        .paymentId("350be6da-10f1-41fe-9840-98c738ec763e")
        .caseReference("sample-case-reference")
        .build();

    return ResponseEntity.ok(fakeResponse);
  }

  @Override
  public PaymentUpdateSuccessResponse updatePaymentStatus(PaymentStatusUpdateRequest request,
      String apiKey) {
    UUID cleanAirZoneId = UUID.fromString(apiKey);
    paymentStatusUpdateService
        .processUpdate(request.toVehicleEntrantPaymentStatusUpdates(cleanAirZoneId));
    return new PaymentUpdateSuccessResponse();
  }
}
