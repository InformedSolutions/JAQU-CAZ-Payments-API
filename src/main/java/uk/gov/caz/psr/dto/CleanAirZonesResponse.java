package uk.gov.caz.psr.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;

/**
 * A value object that represents a response from VCCS request for CleanAirZones details.
 */
@Value
@Builder
public class CleanAirZonesResponse implements Serializable {
  /**
   * Generated serialization id.
   */
  private static final long serialVersionUID = -350641265613442357L;

  List<CleanAirZoneDto> cleanAirZones;
}
