package uk.gov.caz.psr.dto;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import uk.gov.caz.psr.controller.exception.PaymentInfoPaymentMadeDateValidationException;
import uk.gov.caz.psr.dto.validation.constraint.AtLeastOneParameterPresent;
import uk.gov.caz.psr.dto.validation.constraint.FromAndToDatesInChronologicalOrder;
import uk.gov.caz.psr.util.Strings;

@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@AtLeastOneParameterPresent
@FromAndToDatesInChronologicalOrder
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

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info-request.payment-made-date}")
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate paymentMadeDate;

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  @ToString.Include(name = "vrn")
  private String maskedVrn() {
    return Strings.mask(normalizeVrn(vrn));
  }

  /**
   * Throws exception when paymentMadeDate has detected attributes conjunction.
   */
  public void validateParametersConjunction() {
    if (paymentMadeDateHasConjunction()) {
      throw new PaymentInfoPaymentMadeDateValidationException(
          "This parameter cannot be used in conjunction with another other request parameters");
    }
  }

  /**
   * Verifies if provided attributes meet no conjunction requirement.
   *
   * @return boolean
   */
  private boolean paymentMadeDateHasConjunction() {
    return paymentMadeDate != null && anyOtherAttributeIsPresent();
  }

  /**
   * Verifies if attributes other than paymentMadeDate are null.
   *
   * @return boolean
   */
  private boolean anyOtherAttributeIsPresent() {
    return vrn != null
        || paymentProviderId != null
        || fromDatePaidFor != null
        || toDatePaidFor != null;
  }
}
