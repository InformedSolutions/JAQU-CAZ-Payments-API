package uk.gov.caz.psr.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.VehicleEntranceRequest;

@RequestMapping(
    value = PaymentsController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_UTF8_VALUE
)
@Api(value = PaymentsController.BASE_PATH)
public interface PaymentsControllerApiSpec {

  /**
   * Allows Vehicle Compliance Checker to create (unless it already exists) an entry in the database
   * that represents an entrance of a vehicle into a CAZ. Upon completion of this operation the
   * status of the payment associated with the entry is returned.
   *
   * @return {@link PaymentStatusResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.payments.create-vehicle-entrance.description}",
      response = PaymentStatusResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PostMapping(PaymentsController.CREATE_VEHICLE_ENTRANCE_PATH_AND_GET_PAYMENT_DETAILS)
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<Void> createVehicleEntranceAndGetPaymentDetails(
      @RequestBody VehicleEntranceRequest request);
}
