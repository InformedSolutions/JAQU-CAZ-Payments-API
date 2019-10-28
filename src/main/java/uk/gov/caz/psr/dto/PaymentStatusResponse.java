package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class PaymentStatusResponse {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-status}")
  @NotNull
  ChargeSettlementPaymentStatus chargeSettlementPaymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-id}")
  String paymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.case-reference}")
  String caseReference;
}
