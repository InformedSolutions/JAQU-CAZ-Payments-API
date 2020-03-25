package uk.gov.caz.psr.dto;

import java.util.List;
import java.util.UUID;
import lombok.Value;

/**
 * Class that represents the JSON structure for response when getting list of DirectDebitMandates.
 */
@Value
public class AccountDirectDebitMandatesResponse {

  /**
   * The list of DirectDebitMandates associated with the account ID provided in the request.
   */
  List<DirectDebitMandate> directDebitMandates;

  @Value
  public static class DirectDebitMandate {
    UUID directDebitMandateId;

    UUID accountId;

    UUID cleanAirZoneId;

    String paymentProviderMandateId;
  }
}
