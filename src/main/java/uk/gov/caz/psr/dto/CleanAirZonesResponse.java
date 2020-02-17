package uk.gov.caz.psr.dto;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * A value object that represents a response from VCCS request for CleanAirZones details.
 */
@Value
@Builder
public class CleanAirZonesResponse {
  List<CleanAirZoneDto> cleanAirZones;

  @Value
  @Builder
  public static class CleanAirZoneDto {
    UUID cleanAirZoneId;
    String name;
    URI boundaryUrl;
  }
}
