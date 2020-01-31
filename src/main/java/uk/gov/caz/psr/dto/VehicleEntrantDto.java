package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * A class that represents a request from VCCS that a vehicle entered the CAZ.
 */
@Value
@Builder
public class VehicleEntrantDto {

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
}
