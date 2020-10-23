package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Value;
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
}