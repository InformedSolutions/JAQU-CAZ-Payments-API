package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.dto.validation.constraint.ValueIn;

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
  @ValueIn(possibleValues = {"paid", "refunded", "chargeback", "failed"},
      message = "Incorrect payment status update, please use "
          + "\"paid\", \"chargeback\", \"refunded\" or \"failed\" instead")
  String paymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.case-reference}")
  @NotBlank
  @Size(min = 1, max = 15)
  String caseReference;

  public ChargeSettlementPaymentStatus getChargeSettlementPaymentStatus() {
    return ChargeSettlementPaymentStatus.valueOf(paymentStatus.toUpperCase());
  }
}
