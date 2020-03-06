package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;

/**
 * A value object that represents a response from VCCS request for CleanAirZones details.
 */
@Value
@Builder
public class CleanAirZonesResponse {
  List<CleanAirZoneDto> cleanAirZones;
}
