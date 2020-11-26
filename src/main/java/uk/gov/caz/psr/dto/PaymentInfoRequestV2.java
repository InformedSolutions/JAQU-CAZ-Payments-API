package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Value;
import uk.gov.caz.psr.dto.validation.PageNumberValidationException;
import uk.gov.caz.psr.dto.validation.constraint.AtLeastOneParameterPresent;
import uk.gov.caz.psr.dto.validation.constraint.FromAndToDatesInChronologicalOrder;

@Value
@EqualsAndHashCode(callSuper = true)
@AtLeastOneParameterPresent
@FromAndToDatesInChronologicalOrder
public class PaymentInfoRequestV2 extends PaymentInfoRequestV1 {

  private static final int DEFAULT_PAGE_NUMBER = 0;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-info-request.page}")
  Integer page;

  /**
   * Gets the requested page number.
   */
  public int getPage() {
    return Optional.ofNullable(page).orElse(DEFAULT_PAGE_NUMBER);
  }

  /**
   * Throws exception when paymentMadeDate has detected attributes conjunction or page number is not
   * correct.
   */
  public void validate() {
    validateParametersConjunction();
    validatePageNumber();
  }

  /**
   * Throws {@link PageNumberValidationException} if provided page number is negative or above 999.
   */
  private void validatePageNumber() {
    if (page != null && pageNumberIsNotCorrect()) {
      throw new PageNumberValidationException("Page cannot be negative or above than 999");
    }
  }

  /**
   * Verifies if provided page number is negative or above 999.
   */
  private boolean pageNumberIsNotCorrect() {
    return page < 0 || page > 999;
  }
}