package uk.gov.caz.psr.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.psr.dto.GetAndUpdatePaymentStatusResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.PaymentStatusResponse;

@RequestMapping(
    value = PaymentsController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_UTF8_VALUE
)
public interface PaymentsControllerApiSpec {

  /**
   * Allows Payments Front-end to create a new payment based on requested details It creates the
   * payment in GOV.UK PAY and returns next steps which needs to be completed.
   *
   * @return {@link PaymentStatusResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.payments.create-vehicle-entrant.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Payment creation error"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ResponseEntity<InitiatePaymentResponse> initiatePayment(
      @Valid @RequestBody InitiatePaymentRequest request);

  @GetMapping("/{id}")
  ResponseEntity<GetAndUpdatePaymentStatusResponse> getExternalPaymentAndUpdateStatus(
      @PathVariable UUID id);
}
