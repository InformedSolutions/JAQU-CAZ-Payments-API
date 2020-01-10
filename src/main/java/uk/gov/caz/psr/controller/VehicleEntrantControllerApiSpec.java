package uk.gov.caz.psr.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.CazEntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;

/**
 * API specification (swagger) for a controller which deals with requests that informs about a
 * vehicle entering a CAZ.
 */
@RequestMapping(
    value = VehicleEntrantController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface VehicleEntrantControllerApiSpec {

  /**
   * Allows Vehicle Compliance Checker to create (unless it already exists) an entry in the database
   * that represents an entrant of a vehicle into a CAZ. Upon completion of this operation the
   * status of the payment associated with the entry is returned.
   */
  @ApiOperation(
      value = "${swagger.operations.payments.create-vehicle-entrant.description}",
      response = VehicleEntrantDto.class,
      responseContainer = "List"
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
  @PostMapping(VehicleEntrantController.CREATE_VEHICLE_ENTRANT_PATH_AND_GET_PAYMENT_DETAILS)
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<List<CazEntrantPaymentDto>> createVehicleEntrantAndGetPaymentDetails(
      @RequestBody @Valid List<VehicleEntrantDto> vehicleEntrants);
}
