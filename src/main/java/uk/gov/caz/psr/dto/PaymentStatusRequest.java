package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Value;

@Value
public class PaymentStatusRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.vrn}")
  @NotNull
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.date-of-caz-entry}")
  @NotNull
  @Pattern(regexp =  "^\\d{4}-\\d{2}-\\d{2}$")
  String dateOfCazEntry;
}
