package uk.gov.caz.psr.dto.accounts;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a request which updates Direct Debit Mandates statuses.
 */
@Value
@Builder
public class DirectDebitMandatesUpdateRequest {

  /**
   * List of mandateIds to update along with new statuses.
   */
  List<SingleDirectDebitMandateUpdate> directDebitMandates;

  /**
   * Object representing single DirectDebitMandate to update along with the new status.
   */
  @Value
  @Builder
  public static class SingleDirectDebitMandateUpdate {

    /**
     * ID of the mandate to update.
     */
    String mandateId;

    /**
     * New status which will be given to the mandate.
     */
    String status;
  }
}
