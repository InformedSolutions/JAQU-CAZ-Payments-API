package uk.gov.caz.psr.dto.directdebit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.util.Date;
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
    boolean directDebitEnabled;
    List<Mandate> mandates;

    @Value
    @Builder
    public static class Mandate {
      String id;
      String status;
      UUID accountUserId;
      
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      Date created;
    }
  }
}
