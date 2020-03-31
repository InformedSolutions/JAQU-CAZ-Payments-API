package uk.gov.caz.psr.dto.directdebit;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class representing a response from "/v1/payments/accounts/{accountId}/direct-debit-mandates"
 * endpoint.
 */
@Value
@Builder
public class DirectDebitMandatesResponse {

  List<CleanAirZoneWithMandates> cleanAirZones;

  @Value
  @Builder
  public static class CleanAirZoneWithMandates {

    UUID cazId;
    String cazName;
    List<Mandate> mandates;

    @Value
    @Builder
    public static class Mandate {

      String id;
      String status;
    }
  }
}
