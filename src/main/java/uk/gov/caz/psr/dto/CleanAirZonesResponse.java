package uk.gov.caz.psr.dto;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;

/**
 * A value object that represents a response from VCCS request for CleanAirZones details.
 */
@Value
@Builder
public class CleanAirZonesResponse {
  List<CleanAirZoneDto> cleanAirZones;

  /**
   * Method which finds single CleanAirZone with provided ID from the List.
   *
   * @param cleanAirZoneId id of Clean Air Zone which we want to find
   * @return Found CleanAirZone DTO.
   * @throws CleanAirZoneNotFoundException if Clean Air Zone was not found.
   */
  public CleanAirZoneDto findCleanAirZone(UUID cleanAirZoneId) {
    return cleanAirZones.stream()
        .filter(fetch -> fetch.getCleanAirZoneId().equals(cleanAirZoneId))
        .findFirst()
        .orElseThrow(() -> new CleanAirZoneNotFoundException(cleanAirZoneId));
  }

  @Value
  @Builder
  public static class CleanAirZoneDto {
    UUID cleanAirZoneId;
    String name;
    URI boundaryUrl;
  }
}
