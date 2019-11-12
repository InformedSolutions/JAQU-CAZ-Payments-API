package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Value;
import uk.gov.caz.psr.model.VehicleEntrance;

/**
 * A class that represents a request from VCCS that a vehicle entered the CAZ.
 */
@Value
public class VehicleEntranceRequest {

  @ApiModelProperty(
      value = "${swagger.model.descriptions.vehicle-entrance.clean-zone-id}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.vehicle-entrance.caz-entry-timestamp}")
  @NotNull
  LocalDateTime cazEntryTimestamp;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.vehicle-entrance.vrn}")
  @NotNull
  @Size(min = 1, max = 15)
  String vrn;

  /**
   * Maps this value object to an instance of the model.
   *
   * @return An instance of {@link VehicleEntrance} whose parameters comes from
   *         this object.
   */
  public VehicleEntrance toVehicleEntrance() {
    return VehicleEntrance.builder().cleanAirZoneId(cleanAirZoneId)
        .cazEntryTimestamp(cazEntryTimestamp).vrn(vrn).build();
  }
}
