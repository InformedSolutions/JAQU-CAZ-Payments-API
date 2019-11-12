package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Value;
import uk.gov.caz.psr.model.InternalPaymentStatus;

/**
 * A value object that represents the response returned upon the call to vehicle entrant.
 */
@Value
public class VehicleEntrantResponse {
  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle-entrant-response.status}")
  @NotNull
  String status;

  /**
   * Creates an instance of this class based on the passed instance of {@link InternalPaymentStatus}
   * class.
   */
  public static VehicleEntrantResponse from(InternalPaymentStatus internalPaymentStatus) {
    if (internalPaymentStatus == InternalPaymentStatus.PAID) {
      return new VehicleEntrantResponse(internalPaymentStatus.name());
    }

    return new VehicleEntrantResponse(InternalPaymentStatus.NOT_PAID.name());
  }
}
