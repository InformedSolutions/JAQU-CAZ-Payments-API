package uk.gov.caz.psr.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.psr.dto.directdebit.CompleteMandateCreationRequest;

/**
 * Direct Debit mandates redirect flows REST controller API specification.
 */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public interface DirectDebitRedirectFlowsControllerApiSpec {

  /**
   * Completes the process of the mandate creation.
   */
  @PostMapping(DirectDebitRedirectFlowsController.BASE_PATH)
  @ApiOperation(
      value = "${swagger.operations.payments.direct-debit-mandate-complete-creation.description}")
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
  ResponseEntity<Void> completeMandateCreation(@PathVariable("flowId") String flowId,
      @RequestBody CompleteMandateCreationRequest request);
}