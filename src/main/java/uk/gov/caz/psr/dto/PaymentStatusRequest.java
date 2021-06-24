package uk.gov.caz.psr.dto;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.ToString;
import lombok.Value;
import uk.gov.caz.psr.util.Strings;

@Value
public class PaymentStatusRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.vrn}")
  @NotNull
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status.date-of-caz-entry}")
  @NotNull
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
  String dateOfCazEntry;

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  @ToString.Include(name = "vrn")
  private String maskedVrn() {
    return Strings.mask(normalizeVrn(vrn));
  }
}
