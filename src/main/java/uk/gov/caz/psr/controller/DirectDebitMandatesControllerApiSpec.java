package uk.gov.caz.psr.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse;

/**
 * Direct Debit mandates REST controller API specification.
 */
@RequestMapping(value = DirectDebitMandatesController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface DirectDebitMandatesControllerApiSpec {

  /**
   * Gets a list of clean air zones with associates direct debit mandates for a given {@code
   * accountId}.
   */
  @GetMapping
  @ApiOperation(value = "${swagger.operations.payments.direct-debit-mandates.description}",
      response = DirectDebitMandatesResponse.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Clean air zones with associated direct debit mandates"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<DirectDebitMandatesResponse> getDirectDebitMandates(
      @PathVariable("accountId") UUID accountId);

  /**
   * Allows Fleets Front-end to create a new DirectDebitMandate assigned to company.
   * It creates the direct-debit-mandate in GOV.UK PAY and returns next steps which
   * needs to be completed.
   *
   * @return {@link CreateDirectDebitMandateResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.payments.create-vehicle-entrant.description}")
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Direct debit mandate creation error"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header or invalid params")})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "UUID formatted string to track the request through the enquiries stack",
      paramType = "header")})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ResponseEntity<CreateDirectDebitMandateResponse> createDirectDebitMandate(
      @PathVariable("accountId") UUID accountId,
      @RequestBody CreateDirectDebitMandateRequest request);
}
