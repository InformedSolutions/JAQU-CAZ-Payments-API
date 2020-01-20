package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconcilePaymentRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-get-status.clean-air-zone-name}")
  @NotBlank
  @Size(min = 1, max = 58)
  String cleanAirZoneName;

}
