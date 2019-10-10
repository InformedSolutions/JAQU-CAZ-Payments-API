package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

/**
 * A value object which is used as a request for updating payment status.
 */
@Value
public class PaymentStatusUpdateRequest {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.vrn}")
  @NotBlank
  @Max(9)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.status-updates}")
  @NotEmpty
  List<PaymentStatusUpdateDetails> statusUpdates;

  @Value
  private static class PaymentStatusUpdateDetails {

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.date-caz-entry}")
    @NotNull
    LocalDate dateOfCazEntry;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-status}")
    @NotNull
    PaymentStatus paymentStatus;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.case-reference}")
    @NotBlank
    @Max(15)
    String caseReference;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-id}")
    String paymentId;
  }
}
