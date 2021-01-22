package uk.gov.caz.psr.dto.directdebit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class representing a response from
 * "/v1/payments/accounts/{accountId}/direct-debit-mandates/{cleanAirZoneId}" endpoint.
 */
@Value
@Builder
public class DirectDebitMandatesForCazResponse {
  List<Mandate> mandates;

  @Value
  @Builder
  public static class Mandate {
    String id;
    String status;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    Date created;
  }
}
