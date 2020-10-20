package uk.gov.caz.psr.dto.validation.constraint;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import uk.gov.caz.psr.dto.PaymentInfoRequest;

/**
 * Custom validator that checks whether the passed argument contains at least one non-null
 * parameter.
 */
public class AtLeastOneParameterPresentValidator implements
    ConstraintValidator<AtLeastOneParameterPresent, PaymentInfoRequest> {

  /**
   * Checks if the passed {@code input} contains at least on non-null parameter.
   *
   * @param input An input request which will be validated.
   * @param context Validator context (unused).
   * @return true if {@code input} contains at least on non-null parameter, false otherwise.
   */
  @Override
  public boolean isValid(PaymentInfoRequest input, ConstraintValidatorContext context) {
    return input == null
        || input.getFromDatePaidFor() != null
        || input.getToDatePaidFor() != null
        || input.getPaymentProviderId() != null
        || input.getVrn() != null
        || input.getPaymentMadeDate() != null;
  }
}
