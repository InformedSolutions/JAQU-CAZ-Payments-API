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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.PaidPaymentsRequest;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.ReconcilePaymentResponse;

@RequestMapping(
    value = PaymentsController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
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

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<ReconcilePaymentResponse> reconcilePaymentStatus(@PathVariable UUID id);

  /**
   * Allows User to fetch information about already paid days in specific CAZ in order to prevent
   * him from paying for the same day more than one time. Upon completion of this operation the list
   * of provided VRNs along with list of paid days is returned.
   */
  @ApiOperation(
      value = "${swagger.operations.payments.get-paid-entrants.description}",
      response = PaidPaymentsResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 429, message = "Too many requests")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PostMapping(PaymentsController.GET_PAID_VEHICLE_ENTRANTS)
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<PaidPaymentsResponse> getPaidEntrantPayment(
      @RequestBody PaidPaymentsRequest paymentsRequest);
  
  /**
   * Endpoint for retrieving a summary list of clean air zones and their
   * boundary URLs. Note this endpoint acts as a proxy through to the Vehicle
   * Checker service.
   * 
   * @return a summary listing of a clean air zone including their identifiers
   *         and boundary urls.
   */
  @GetMapping(PaymentsController.GET_CLEAN_AIR_ZONES)
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
  ResponseEntity<CleanAirZonesResponse> getCleanAirZones();
  
  /**
   * Get vehicle compliance details.
   *
   * @param vrn validated string
   * @return Vehicle details about car
   */
  @ApiOperation(value = "${swagger.operations.vehicle.compliance.description}",
      response = ComplianceResultsDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle compliance details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("vehicles/{vrn}/compliance")
  ResponseEntity<ComplianceResultsDto> getCompliance(@PathVariable String vrn, 
      @RequestParam("zones") String zones);
}
