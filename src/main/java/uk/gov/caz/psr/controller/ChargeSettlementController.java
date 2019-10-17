package uk.gov.caz.psr.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.dto.PaymentUpdateSuccessResponse;

/**
 * A controller which exposes endpoints dealing with charge settlement.
 */
@RestController
@Slf4j
public class ChargeSettlementController implements ChargeSettlementControllerApiSpec {

  static final String BASE_PATH = "/v1/charge-settlement";
  static final String PAYMENT_INFO_PATH = "/payment-info";
  static final String PAYMENT_STATUS_PATH = "/payment-status";

  @Override
  public ResponseEntity<PaymentInfoResponse> getPaymentInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PaymentUpdateSuccessResponse updatePaymentStatus(PaymentStatusUpdateRequest request) {
    throw new UnsupportedOperationException();
  }
}
