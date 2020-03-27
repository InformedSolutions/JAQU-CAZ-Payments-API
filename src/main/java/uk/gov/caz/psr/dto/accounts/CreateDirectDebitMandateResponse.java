package uk.gov.caz.psr.dto.accounts;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents JSON response from Accounts API after DirectDebitMandate creation.
 */
@Value
@Builder
public class CreateDirectDebitMandateResponse {
  /**
   * Primary key for T_ACCOUNT_DIRECT_DEBIT_MANDATE table.
   */
  UUID directDebitMandateId;

  /**
   * An identifier of the associated Account.
   */
  UUID accountId;

  /**
   * An identifier of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * Identifier generated for the mandate by GOV.UK Pay
   */
  String paymentProviderMandateId;
}
