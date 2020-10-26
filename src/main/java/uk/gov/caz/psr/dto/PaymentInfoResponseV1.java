package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * A value object which is used as a response in the endpoint that returns details about the
 * specific payments.
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@RequiredArgsConstructor
public class PaymentInfoResponseV1 {
  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.results}")
  List<PaymentsInfo> results;

  @Value
  public static class PaymentsInfo {
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.vrn}")
    String vrn;
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payments}")
    List<SinglePaymentInfo> payments;
  }

  @Value
  @Builder
  public static class SinglePaymentInfo {
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.caz-payment-reference}")
    Long cazPaymentReference;
    
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-provider-id}")
    String paymentProviderId;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-date}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate paymentDate;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.total-paid}")
    BigDecimal totalPaid;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-method}")
    ChargeSettlementPaymentMethod paymentMethod;

    @Nullable
    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-mandate-id}")
    String paymentMandateId;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.telephone-payment}")
    boolean telephonePayment;

    @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.line-items}")
    List<VehicleEntrantPaymentInfo> lineItems;

    @Value
    @Builder
    public static class VehicleEntrantPaymentInfo {

      @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.payment-status}")
      ChargeSettlementPaymentStatus paymentStatus;

      @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.case-reference}")
      String caseReference;

      @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.charge-paid}")
      BigDecimal chargePaid;

      @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.travel-date}")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      LocalDate travelDate;
    }
  }
}
