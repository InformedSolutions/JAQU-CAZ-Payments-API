package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentResponse;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CreateDirectDebitPaymentService;
import uk.gov.caz.psr.util.DirectDebitPaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;

@RestController
@AllArgsConstructor
public class DirectDebitPaymentsController implements DirectDebitPaymentsControllerApiSpec {

  @VisibleForTesting
  public static final String BASE_PATH = "/v1/direct-debit-payments";

  private final CreateDirectDebitPaymentService createDirectDebitPaymentService;

  @Override
  public ResponseEntity<CreateDirectDebitPaymentResponse> createPayment(
      CreateDirectDebitPaymentRequest request) {
    request.validate();
    Payment payment = createDirectDebitPaymentService.createPayment(
        DirectDebitPaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions()),
        request.getMandateId()
    );
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateDirectDebitPaymentResponse.from(payment));
  }
}
