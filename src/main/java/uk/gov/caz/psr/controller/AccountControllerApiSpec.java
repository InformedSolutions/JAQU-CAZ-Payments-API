package uk.gov.caz.psr.controller;

import static uk.gov.caz.psr.controller.AccountsController.ACCOUNTS_PATH;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;

@RequestMapping(value = ACCOUNTS_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE})
public interface AccountControllerApiSpec {
  
  /**
   * An endpoint to retrieve pages of vehicles and their tariffs associated with 
   * a given account.
   *
   * @return {@link VehicleRetrievalResponseDto} wrapped in {@link ResponseEntity}.
   * @throws IOException if external service cannot be reached
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.vehicle-retrieval.description}",
      response = VehicleRetrievalResponseDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Bad Request (the request is missing a mandatory "
          + "element)"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 429, message = "Too many requests"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "account_id",
          required = true,
          value = "The identifier of the account to retrieve vehicles for",
          paramType = "path"),
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "pageNumber",
          required = true, 
          value = "The number of the page to retrieve",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize",
          required = true,
          value = "The size of the page to retrieve",
          paramType = "query"),
      @ApiImplicitParam(name = "zones",
          required = true,
           value = "The clean air zones for which to return charges",
           paramType = "query")
  })
  @GetMapping("/{accountId}/charges")
  ResponseEntity<VehicleRetrievalResponseDto> retrieveVehiclesAndCharges(
      @PathVariable("accountId") UUID accountId, 
      @RequestParam(required = true) Map<String, String> queryStrings) throws IOException;
  
}