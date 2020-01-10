package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * A value object that stores data after creation/update of CAZ_ENTRANT_PAYMENT table.
 */
@Value
@Builder
public class CazEntrantPaymentDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.vehicle-entrant-id")
  @NotNull
  UUID vehicleEntrantId;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.status}")
  @NotNull
  String paymentStatus;
}
