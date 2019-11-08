package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.VehicleEntrant;

/**
 * A class that represents a request from VCCS that a vehicle entered the CAZ.
 */
@Value
@Builder
public class VehicleEntrantRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrant.clean-zone-id}")
  @NotNull
  UUID cleanZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrant.caz-entry-timestamp}")
  @NotNull
  LocalDateTime cazEntryTimestamp;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrant.vrn}")
  @NotNull
  @Size(min = 1, max = 15)
  String vrn;

  /**
   * Maps this value object to an instance of the model.
   *
   * @return An instance of {@link VehicleEntrant} whose parameters comes from this object.
   */
  public VehicleEntrant toVehicleEntrant() {
    return VehicleEntrant.builder()
        .cleanZoneId(cleanZoneId)
        .cazEntryTimestamp(cazEntryTimestamp)
        .vrn(vrn)
        .build();
  }
}
