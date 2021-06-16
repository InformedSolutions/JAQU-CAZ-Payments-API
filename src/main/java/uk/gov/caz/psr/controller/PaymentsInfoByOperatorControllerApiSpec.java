package uk.gov.caz.psr.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByDatesRequest;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorRequest;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorResponse;

/**
 * Controller that exposes endpoints related to obtaining information about payments
 * made by operators.
 */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public interface PaymentsInfoByOperatorControllerApiSpec {

  /**
   * Gets information about payments made by the given operator represented by their identifier.
   */
  @GetMapping(PaymentsInfoByOperatorController.OPERATOR_HISTORY_PATH)
  @ApiOperation(
      value = "${swagger.operations.payments-info-by-operator-id.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Payments information for the given operator")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  ResponseEntity<PaymentsInfoByOperatorResponse> getPaymentsByOperatorId(
      @PathVariable String operatorId, PaymentsInfoByOperatorRequest request);

  /**
   * Gets information about payments made by call operators in a given date range.
   */
  @GetMapping(PaymentsInfoByOperatorController.OPERATORS_HISTORY_PATH)
  @ApiOperation(
      value = "${swagger.operations.payments-info-by-dates.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Payments information by call operators in a given date "
          + "range."),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "startDate",
          required = true,
          value = "Date that describes payment date from",
          paramType = "query"),
      @ApiImplicitParam(name = "endDate",
          required = true,
          value = "Date that describes payment date until",
          paramType = "query"),
      @ApiImplicitParam(name = "pageNumber",
          required = true,
          value = "The number of the page to be retrieved",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize",
          required = true,
          value = "The size of the page to be retrieved",
          paramType = "query"),
  })
  ResponseEntity<PaymentsInfoByOperatorResponse> getPaymentsByDates(
      PaymentsInfoByDatesRequest request);
}
