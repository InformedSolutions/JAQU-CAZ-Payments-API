package uk.gov.caz.psr.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.QueryParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.VehiclePaymentHistoryResponse;

/**
 * API specification (swagger) for a controller which returns payment history for VRN.
 */
@RequestMapping(
    produces = MediaType.APPLICATION_JSON_VALUE,
    value = VehiclePaymentHistoryController.BASE_PATH
)
@Validated
public interface VehiclePaymentHistoryControllerApiSpec {

  /**
   * Fetches vehicle historical payments.
   * @param vrn vehicle identification
   * @param pageNumber page number to get
   * @param pageSize size of page
   * @return historical entries
   */
  @ApiOperation(
      value = "${swagger.operations.payments.get-payment-history-for-vehicle}",
      response = VehiclePaymentHistoryResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element or its value is wrong)"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "vrn",
          required = true,
          value = "The identifier of the vehicle to retrieve history for",
          paramType = "path"),
      @ApiImplicitParam(name = "pageNumber",
          value = "The number of the page to retrieve",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize",
          value = "The size of the page to retrieve",
          paramType = "query")
  })
  @GetMapping(VehiclePaymentHistoryController.GET_VEHICLE_HISTORY)
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<VehiclePaymentHistoryResponse> historyForVehicle(
      @PathVariable("vrn") String vrn, @QueryParam("pageNumber") Integer pageNumber,
      @QueryParam("pageSize") Integer pageSize);
}
