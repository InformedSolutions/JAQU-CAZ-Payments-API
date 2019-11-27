package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.Value;

/**
 * A value object which is used as a response in the endpoint that returns details about the
 * specific payments.
 */
@Value
public class PaymentInfoResponse {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.results}")
  List<PaymentInfoResults> results;

  @Value
  public static class PaymentInfoResults {
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.vrn}")
    String vrn;
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payments}")
    List<PaymentsInfo> payments;
  }

  @Value
  public static class PaymentsInfo {
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-id}")
    String paymentId;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-date}")
    LocalDate paymentDate;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-status}")
    ChargeSettlementPaymentStatus chargeSettlementPaymentStatus;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.case-reference}")
    String caseReference;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.charge-paid}")
    double chargePaid;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.travel-dates}")
    List<LocalDate> travelDates;
  }
}
