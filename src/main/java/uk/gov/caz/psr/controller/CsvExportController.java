package uk.gov.caz.psr.controller;

import com.google.common.base.Stopwatch;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.PaymentHistoryCsvExportResponse;
import uk.gov.caz.psr.service.generatecsv.PaymentsHistoryCsvFileSupervisor;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CsvExportController implements CsvExportControllerApiSpec {

  public static final String CSV_EXPORT_PATH = "/v1/accounts/{accountId}/payments/csv-exports";

  private final PaymentsHistoryCsvFileSupervisor paymentsHistoryCsvFileSupervisor;

  @Value("${csv-export-bucket}")
  private String bucketName;

  @Override
  public ResponseEntity<PaymentHistoryCsvExportResponse> generatePaymentHistoryCsv(UUID accountId,
      UUID accountUserId) {
    log.info("Starting export of Payments as CSV into AWS S3");
    Stopwatch timer = Stopwatch.createStarted();

    URL url = paymentsHistoryCsvFileSupervisor
        .uploadCsvFileAndGetPresignedUrl(accountId, accountUserId);

    log.info("Exporting Payment into AWS S3 took {} ms",
        timer.stop().elapsed(TimeUnit.MILLISECONDS));
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(PaymentHistoryCsvExportResponse.builder()
            .bucketName(bucketName)
            .fileUrl(url)
            .build());
  }
}
