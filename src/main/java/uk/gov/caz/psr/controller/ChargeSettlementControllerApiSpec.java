package uk.gov.caz.psr.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentStatusRequest;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.dto.PaymentUpdateSuccessResponse;

@RequestMapping(
    value = ChargeSettlementController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_UTF8_VALUE
)
public interface ChargeSettlementControllerApiSpec {

  /**
   * Allows LAs to query and retrieve data about a payment that has been made via GOV.UK Pay in
   * relation to a vehicle that has entered their CAZ (a 'vehicle entrant').
   *
   * @return {@link PaymentInfoResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.charge-settlement.payment-info.description}",
      response = PaymentInfoResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "Timestamp",
          required = true,
          value = "ISO 8601 formatted datetime string indicating time that the request was "
              + "initialised",
          paramType = "header"),
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "x-api-key",
          required = true, value = "API key used to access the service",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization",
          required = true,
          value = "OAuth 2.0 authorisation token",
          paramType = "header")
  })
  @GetMapping(ChargeSettlementController.PAYMENT_INFO_PATH)
  ResponseEntity<PaymentInfoResponse> getPaymentInfo();

  /**
   * Allows LAs to query and retrieve data that enables them to determine whether a vehicle that has
   * entered their CAZ (a 'vehicle entrant') has paid the charge that they are liable for in order
   * to determine whether enforcement action is required.
   *
   * @return {@link PaymentStatusResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.charge-settlement.payment-status.description}",
      response = PaymentStatusResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "Timestamp",
          required = true,
          value = "ISO 8601 formatted datetime string indicating time that the request was "
              + "initialised",
          paramType = "header"),
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "x-api-key",
          required = true, value = "API key used to access the service",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization",
          required = true,
          value = "OAuth 2.0 authorisation token",
          paramType = "header")
  })
  @GetMapping(ChargeSettlementController.PAYMENT_STATUS_PATH)
  ResponseEntity<PaymentStatusResponse> getPaymentStatus(@Valid PaymentStatusRequest request);

  /**
   * Allows Local Authorities to update the status of one or more paid CAZ charges to reflect any
   * action that is being taken with the payment (e.g. such as the processing of a refund or
   * chargeback).
   *
   * @return An instance of {@link PaymentUpdateSuccessResponse}.
   */
  @ApiOperation(
      value = "${swagger.operations.charge-settlement.payment-status-update.description}",
      response = PaymentUpdateSuccessResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "Timestamp",
          required = true,
          value = "ISO 8601 formatted datetime string indicating time that the request was "
              + "initialised",
          paramType = "header"),
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "x-api-key",
          required = true, value = "API key used to access the service",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization",
          required = true,
          value = "OAuth 2.0 authorisation token",
          paramType = "header")
  })
  @PutMapping(ChargeSettlementController.PAYMENT_STATUS_PATH)
  PaymentUpdateSuccessResponse updatePaymentStatus(@RequestBody PaymentStatusUpdateRequest request);
}
