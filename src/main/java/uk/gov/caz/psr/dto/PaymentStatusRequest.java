package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Value
public class PaymentStatusRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.vrn}")
  @NotNull
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.date-of-caz-entry}")
  @NotNull
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate dateOfCazEntry;
}
