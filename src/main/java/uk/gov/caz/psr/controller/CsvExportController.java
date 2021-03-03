package uk.gov.caz.psr.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.PaymentHistoryCsvExportResponse;

@RestController
public class CsvExportController implements CsvExportControllerApiSpec {

  public static final String CSV_EXPORT_PATH = "/v1/accounts/{accountId}/payments/csv-exports";

  @Override
  public ResponseEntity<PaymentHistoryCsvExportResponse> generatePaymentHistoryCsv(UUID accountId,
      UUID accountUserId) {

    // TODO: CAZB-4139 csv generation service

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(PaymentHistoryCsvExportResponse.builder()
            .bucketName("some-bucket-name")
            .fileUrl("http://example.com")
            .build());
  }
}
