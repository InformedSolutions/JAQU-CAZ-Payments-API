package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * A value object which is used as a request for updating payment status.
 */
@Value
@Builder
public class PaymentStatusUpdateRequest {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.vrn}")
  @NotBlank
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.status-updates}")
  @NotEmpty
  List<PaymentStatusUpdateDetails> statusUpdates;
}
