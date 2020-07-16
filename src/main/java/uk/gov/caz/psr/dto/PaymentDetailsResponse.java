package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentDetailsResponse {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details"
      + ".central-payment-reference}")
  Long centralPaymentReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.payment-provider-id}")
  String paymentProviderId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.payment-date}")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate paymentDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.total-paid}")
  BigDecimal totalPaid;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.telephone-payment}")
  boolean telephonePayment;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.line-items}")
  List<VehicleEntrantPaymentDetails> lineItems;

  @Value
  @Builder
  public static class VehicleEntrantPaymentDetails {

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.case-reference}")
    String caseReference;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.charge-paid}")
    BigDecimal chargePaid;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.payment-status}")
    ChargeSettlementPaymentStatus paymentStatus;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-details.travel-date}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate travelDate;
  }
}
