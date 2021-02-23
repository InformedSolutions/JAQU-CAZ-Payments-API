package uk.gov.caz.psr.dto.accounts;

import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Value class that represents a request body to Accounts service to create DirectDebitMandate.
 */
@Value
@Builder
public class CreateDirectDebitMandateRequest {
  /**
   * An identifier of mandate.
   */
  @NonNull
  String mandateId;

  /**
   * An identifier of the Clean Air Zone.
   */
  @NonNull
  UUID cleanAirZoneId;

  /**
   * An indentifier of the Debit creator.
   */
  @NonNull
  UUID accountUserId;
}
