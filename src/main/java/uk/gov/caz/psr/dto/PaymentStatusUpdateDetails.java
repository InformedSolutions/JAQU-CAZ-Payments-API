package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * A value object which is used as a request for updating payment status which contains payment
 * Details.
 */
@Value
@Builder
public class PaymentStatusUpdateDetails {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.date-caz-entry}")
  @NotNull
  LocalDate dateOfCazEntry;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-status}")
  @NotNull
  ChargeSettlementPaymentStatus chargeSettlementPaymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-id}")
  @Max(255)
  String paymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.case-reference}")
  @NotBlank
  @Max(15)
  String caseReference;
}
