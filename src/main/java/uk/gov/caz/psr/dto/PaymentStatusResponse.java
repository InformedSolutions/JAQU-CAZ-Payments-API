package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModelProperty;
import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.PaymentMethod;
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

  @Nullable
  @JsonInclude(Include.NON_NULL)
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.payment-mandate-id}")
  String paymentMandateId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.telephone-payment}")
  boolean telephonePayment;

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
        .paymentMandateId(
            PaymentMethod.DIRECT_DEBIT.equals(paymentStatus.getPaymentMethod())
                ? paymentStatus.getPaymentProviderMandateId()
                : null
        )
        .telephonePayment(paymentStatus.isTelephonePayment())
        .build();
  }
  
  
  /**
   * Helper method to create ${@link PaymentStatusResponse} when entrant is not found.
   *
   * @return {@link PaymentStatusResponse}
   */
  public static PaymentStatusResponse notFound() {
    return PaymentStatusResponse.builder()
        .paymentStatus(ChargeSettlementPaymentStatus.NOT_PAID)
        .build();
  }
}
