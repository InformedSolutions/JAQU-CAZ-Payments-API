package uk.gov.caz.psr.dto.accounts;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Value object representing a request which updates payment history export job.
 */
@Value
@Builder
public class UpdatePaymentHistoryExportRequest {

  /**
   * A URL of exported payment history file.
   */
  @NonNull
  String fileUrl;

  /**
   * The status of updated job.
   */
  @NonNull
  String status;
}
