package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.ResendReceiptEmailRequest;
import uk.gov.caz.psr.service.PaymentReceiptService;

@RestController
@AllArgsConstructor
public class PaymentReceiptController implements PaymentReceiptControllerApiSpec {

  private final PaymentReceiptService paymentReceiptService;

  @VisibleForTesting
  public static final String BASE_PATH = "/v1/payments/{payment_reference}";

  @VisibleForTesting
  public static final String RESEND_RECEIPT_EMAIL = "receipts";

  @Override
  public ResponseEntity<Void> resendReceiptEmail(Long referenceNumber,
      ResendReceiptEmailRequest request) {
    request.validate();
    paymentReceiptService.sendReceipt(referenceNumber, request.getEmail());
    return ResponseEntity.ok().build();
  }
}
