package uk.gov.caz.psr.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javassist.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.vccs.RegisterDetailsDto;
import uk.gov.caz.psr.dto.whitelist.WhitelistedVehicleResponseDto;

/**
 * API specification for endpoints that act as a proxy to other services.
 */
@RequestMapping(value = PaymentsController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface ProxyControllerApiSpec {

  /**
   * Endpoint for retrieving a summary list of clean air zones and their boundary URLs. Note this
   * endpoint acts as a proxy through to the Vehicle Checker service.
   *
   * @return a summary listing of a clean air zone including their identifiers and boundary urls.
   * @throws JsonProcessingException exception encountered whilst serializing/deserializing JSON
   *                                 payloads
   */
  @GetMapping(ProxyController.GET_CLEAN_AIR_ZONES)
  @ApiOperation(value = "${swagger.operations.cleanAirZones.description}",
      response = CleanAirZonesDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Clean air zone listing details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  ResponseEntity<CleanAirZonesDto> getCleanAirZones() throws JsonProcessingException;

  /**
   * Get vehicle details.
   *
   * @param vrn validated string
   * @return Vehicle details about car
   */
  @ApiOperation(value = "${swagger.operations.vehicle.details.description}",
      response = VehicleDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping(ProxyController.GET_VEHICLE_DETAILS)
  ResponseEntity<VehicleDto> getVehicleDetails(@PathVariable String vrn);


  /**
   * Get vehicle compliance details.
   *
   * @param vrn validated string
   * @return Vehicle compliance details
   */
  @ApiOperation(value = "${swagger.operations.vehicle.compliance.description}",
      response = ComplianceResultsDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle compliance details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping(ProxyController.GET_COMPLIANCE)
  ResponseEntity<ComplianceResultsDto> getCompliance(@PathVariable String vrn,
      @RequestParam("zones") String zones);

  /**
   * Get compliance details in bulk.
   */
  @ApiOperation(value = "${swagger.operations.vehicle.bulk-compliance.description}",
      response = List.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Err/ No message available"),
      @ApiResponse(code = 400, message = "vrns missing or empty"),
      @ApiResponse(code = 200, message = "Compliance details for multiple vehicles")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "X-Correlation-ID", required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @PostMapping(ProxyController.POST_BULK_COMPLIANCE)
  ResponseEntity<List<ComplianceResultsDto>> bulkCompliance(
      @RequestParam(value = "zones", required = false) String zones,
      @RequestBody List<String> vrns);

  /**
   * Get charges for given type.
   *
   * @param type non-null string
   */
  @ApiOperation(value = "${swagger.operations.vehicle.unrecognised.description}",
      response = VehicleTypeCazChargesDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "type param missing"),
      @ApiResponse(code = 400, message = "zones parameter malformed"),
      @ApiResponse(code = 200, message = "Vehicle compliance details")})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping(ProxyController.GET_UNRECOGNISED_VEHICLE_COMPLIANCE)
  ResponseEntity<VehicleTypeCazChargesDto> getUnrecognisedVehicle(@PathVariable("type") String type,
      @RequestParam("zones") String zones) throws NotFoundException;

  /**
   * Gets details of a whitelisted vehicle by its {@code vrn}. This is a proxy to the whitelist
   * service.
   */
  @ApiOperation(value = "${swagger.operations.whitelist.vehicle-details.description}",
      response = WhitelistedVehicleResponseDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 404, message = "Vehicle details not found"),
      @ApiResponse(code = 200, message = "Vehicle compliance details")})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through the Enquiries stack",
      paramType = "header")})
  @GetMapping(ProxyController.GET_WHITELIST_VEHICLE_DETAILS)
  ResponseEntity<WhitelistedVehicleResponseDto> getWhitelistVehicleDetails(
      @PathVariable("vrn") String vrn);

  /**
   * Get register details for vehicle.
   *
   * @param vrn string
   * @return Registered details about car
   */
  @ApiOperation(value = "${swagger.operations.register.details.description}",
      response = RegisterDetailsDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle registered details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping(ProxyController.GET_REGISTER_DETAILS)
  ResponseEntity<RegisterDetailsDto> getRegisterDetails(@PathVariable String vrn);
}
