package uk.gov.caz.psr.dto;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for response when getting list of DirectDebitMandates.
 */
@Value
@Builder
public class AccountDirectDebitMandatesResponse {

  /**
   * The list of DirectDebitMandates associated with the account ID provided in the request.
   */
  List<DirectDebitMandate> directDebitMandates;

  @Value
  @Builder
  public static class DirectDebitMandate {
    UUID directDebitMandateId;

    UUID accountId;

    UUID cleanAirZoneId;

    String paymentProviderMandateId;

    DirectDebitMandateStatus status;

    Date created;
    
    public enum DirectDebitMandateStatus {
      CREATED,
      STARTED,
      ABANDONED,
      FAILED,
      PENDING,
      ACTIVE,
      CANCELLED,
      INACTIVE,
      ERROR
    }
  }
}
