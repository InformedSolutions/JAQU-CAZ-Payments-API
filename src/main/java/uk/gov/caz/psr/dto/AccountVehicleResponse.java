package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents JSON response from a single vehicle lookup.
 */
@Value
@Builder
public class AccountVehicleResponse {

  @ApiModelProperty(value = "${swagger.model.descriptions.account-vehicle.vrn}")
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.account-vehicle.account-id")
  UUID accountId;

}
