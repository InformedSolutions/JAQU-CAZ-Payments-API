package uk.gov.caz.psr.model.directdebit;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Holder for Clean Air Zone with direct debit mandates.
 */
@Builder
@Value
public class CleanAirZoneWithMandates {

  UUID cleanAirZoneId;
  String cazName;
  List<Mandate> mandates;
}
