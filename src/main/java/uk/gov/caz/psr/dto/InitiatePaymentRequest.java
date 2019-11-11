package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.dto.validation.constraint.AmountDivisibleByNumberOfDays;

@Value
@Builder
@AmountDivisibleByNumberOfDays
public class InitiatePaymentRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.clean-zone-id}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.days}")
  @NotNull
  @Size(min = 1, max = 13)
  List<LocalDate> days;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.vrn}")
  @NotBlank
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.amount}")
  @NotNull
  @Positive
  Integer amount;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.return-url}")
  @NotBlank
  String returnUrl;
}
