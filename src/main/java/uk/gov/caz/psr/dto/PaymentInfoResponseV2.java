package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A value object which is used as a response in the endpoint that returns details about the
 * specific payments.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PaymentInfoResponseV2 extends PaymentInfoResponseV1 {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info.pages}")
  int pages;

  public PaymentInfoResponseV2(List<PaymentsInfo> paymentsInfo, int pages) {
    super(paymentsInfo);
    this.pages = pages;
  }
}