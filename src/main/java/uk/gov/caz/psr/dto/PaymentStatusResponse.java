package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.PaymentStatus;

@Value
@Builder
public class PaymentStatusResponse {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-status}")
  @NotNull
  ChargeSettlementPaymentStatus paymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.caz-payment-reference}")
  @NotNull
  Long cazPaymentReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-provider-id}")
  @Max(255)
  String paymentProviderId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.case-reference}")
  String caseReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-method}")
  ChargeSettlementPaymentMethod paymentMethod;

  /**
   * Helper method to map {@link PaymentStatus} to ${@link PaymentStatusResponse}.
   *
   * @param paymentStatus {@link PaymentStatus}
   * @return {@link PaymentStatusResponse}
   */
  public static PaymentStatusResponse from(PaymentStatus paymentStatus) {
    return PaymentStatusResponse.builder()
        .paymentStatus(ChargeSettlementPaymentStatus.from(paymentStatus.getStatus()))
        .cazPaymentReference(paymentStatus.getPaymentReference())
        .paymentProviderId(paymentStatus.getExternalId())
        .caseReference(paymentStatus.getCaseReference())
        .paymentMethod(ChargeSettlementPaymentMethod.from(paymentStatus.getPaymentMethod()))
        .build();
  }
}
