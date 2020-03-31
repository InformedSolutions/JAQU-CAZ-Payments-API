package uk.gov.caz.psr.dto.directdebit;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse.CleanAirZoneWithMandates.Mandate;

/**
 * Class representing a response from
 * "/v1/payments/accounts/{accountId}/direct-debit-mandates/{cleanAirZoneId}" endpoint.
 */
@Value
@Builder
public class DirectDebitMandatesForCazResponse {
  List<Mandate> mandates;
}
