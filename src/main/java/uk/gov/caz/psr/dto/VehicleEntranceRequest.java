package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class VehicleEntranceRequest {
  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrance.clean-zone-id}")
  @NotNull
  UUID cleanZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrance.date-of-entrance}")
  @NotNull
  LocalDate dateOfEntrance;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrance.vrn}")
  @NotBlank
  String vrn;
}
