package uk.gov.caz.psr.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentInfoResponse.PaymentInfoResults;
import uk.gov.caz.psr.dto.PaymentInfoResponse.PaymentsInfo;
import uk.gov.caz.psr.dto.PaymentStatusRequest;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.dto.PaymentUpdateSuccessResponse;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.service.ChargeSettlementService;
import uk.gov.caz.psr.service.PaymentStatusUpdateService;

/**
 * A controller which exposes endpoints dealing with charge settlement.
 */
@RestController
@AllArgsConstructor
public class ChargeSettlementController implements ChargeSettlementControllerApiSpec {

  public static final String BASE_PATH = "/v1/charge-settlement";
  public static final String PAYMENT_INFO_PATH = "/payment-info";
  public static final String PAYMENT_STATUS_PATH = "/payment-status";

  private final PaymentStatusUpdateService paymentStatusUpdateService;
  private final ChargeSettlementService chargeSettlementService;

  @Override
  public ResponseEntity<PaymentInfoResponse> getPaymentInfo(PaymentInfoRequest paymentInfoRequest) {
//    PaymentsInfo paymentsInfo = new PaymentsInfo(
//        "paymentId",
//        LocalDate.now(),
//        ChargeSettlementPaymentStatus.PAID,
//        "caseReference",
//        54.5,
//        Collections.singletonList(LocalDate.now())
//    );
//    PaymentInfoResults paymentInfoResults = new PaymentInfoResults(
//        "vrn",
//        Collections.singletonList(paymentsInfo)
//    );
    return null;
//    return ResponseEntity.ok(new PaymentInfoResponse(
//        Collections.singletonList(paymentInfoResults)));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus(PaymentStatusRequest request,
      String apiKey) {
    UUID cleanAirZoneId = UUID.fromString(apiKey);

    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(
            cleanAirZoneId,
            request.getVrn(),
            request.getDateOfCazEntry());

    return ResponseEntity.ok(PaymentStatusResponse.from(paymentStatus));
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
