package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import uk.gov.caz.psr.dto.validation.constraint.AtLeastOneParameterPresent;

@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@AtLeastOneParameterPresent
public class PaymentInfoRequest {

  @ApiModelProperty(value =
      "${swagger.model.descriptions.payment-info-request.payment-provider-id}")
  @Size(min = 1, max = 255)
  String paymentProviderId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info-request.vrn}")
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info-request.from-date-paid-for}")
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate fromDatePaidFor;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info-request.to-date-paid-for}")
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate toDatePaidFor;
}
