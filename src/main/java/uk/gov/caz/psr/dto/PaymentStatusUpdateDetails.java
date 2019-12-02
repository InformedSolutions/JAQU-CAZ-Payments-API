package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.ValidationError;

/**
 * A value object which is used as a request for updating payment status which contains payment
 * Details.
 */
@Value
@Builder
public class PaymentStatusUpdateDetails {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.date-caz-entry}")
  @NotNull(message = ValidationError.MANDATORY_FIELD_MISSING_ERROR)
  LocalDate dateOfCazEntry;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-status}")
  @NotNull(message = ValidationError.MANDATORY_FIELD_MISSING_ERROR)
  ChargeSettlementPaymentStatus chargeSettlementPaymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.payment-id}")
  @Size(max = 255)
  String paymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.case-reference}")
  @NotBlank(message = ValidationError.MANDATORY_FIELD_MISSING_ERROR)
  @Size(max = 15)
  String caseReference;
}
