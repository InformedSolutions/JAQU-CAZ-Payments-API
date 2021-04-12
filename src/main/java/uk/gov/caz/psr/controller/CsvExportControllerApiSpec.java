package uk.gov.caz.psr.controller;

import static uk.gov.caz.psr.controller.CsvExportController.CSV_EXPORT_PATH;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.PaymentHistoryCsvExportResponse;

@RequestMapping(
    value = CSV_EXPORT_PATH,
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public interface CsvExportControllerApiSpec {

  /**
   * Returns the details of an exported csv file.
   *
   * @return {@link PaymentHistoryCsvExportResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(value = "${swagger.operations.export-payments-history-to-csv.description}")
  @ApiResponses({
      @ApiResponse(code = 201, message = "File exported to S3"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation ID Header"),
      @ApiResponse(code = 404, message = "Account was not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = Constants.X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "The identifier of the account to retrieve payments data",
          paramType = "path"),
      @ApiImplicitParam(name = "accountUserId",
          required = false,
          value = "The identifier of the account user",
          paramType = "query")
  })
  @PostMapping
  ResponseEntity<PaymentHistoryCsvExportResponse> generatePaymentHistoryCsv(
      @PathVariable("accountId") UUID accountId,
      @RequestParam(required = false) UUID accountUserId);
}
