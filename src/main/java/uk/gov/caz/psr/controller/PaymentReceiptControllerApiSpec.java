package uk.gov.caz.psr.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.PaymentReceiptController.RESEND_RECEIPT_EMAIL;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.psr.dto.ResendReceiptEmailRequest;

@RequestMapping(value = PaymentReceiptController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface PaymentReceiptControllerApiSpec {

  /**
   * Allows to resend a receipt to user's email provided in the payload.
   */
  @ApiOperation(value = "${swagger.operations.payments.resend-receipt-email.description}")
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 404, message = "Payment not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Email was successfully sent")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "payment_reference",
          required = true,
          value = "Identifies the payment",
          paramType = "path")
  })
  @PostMapping(RESEND_RECEIPT_EMAIL)
  ResponseEntity<Void> resendReceiptEmail(
      @PathVariable("payment_reference") Long referenceNumber,
      @RequestBody ResendReceiptEmailRequest request);
}
