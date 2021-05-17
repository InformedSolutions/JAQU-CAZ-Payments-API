package uk.gov.caz.psr.dto;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

/**
 * Class that keeps input object parameters for Lambda which exporting payment history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentsHistoryLambdaInput {

  /**
   * Correlation ID connected with this request.
   */
  private UUID correlationId;

  /**
   * Job of id to be marked after exporting payment history.
   */
  private Integer registerJobId;

  /**
   * An identifier of the associated account.
   */
  private UUID accountId;

  /**
   * List of account user ids for which we should generate payment history.
   */
  private List<UUID> accountUserIds;

  /**
   * Validates this dto.
   */
  public void validate() {
    Preconditions.checkNotNull(correlationId, "correlationId has to be set");
    Preconditions.checkNotNull(registerJobId, "registerJobId has to be set");
    Preconditions.checkNotNull(accountId, "accountId has to be set");
    Preconditions.checkArgument(!CollectionUtils.isEmpty(accountUserIds),
        "accountUserIds cannot be null or empty");
  }
}
