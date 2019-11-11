package uk.gov.caz.psr.dto.validation.constraint;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;

/**
 * Custom validator that checks whether the amount is divisible by the number of days.
 */
public class AmountDivisibleByNumberOfDaysValidator implements
    ConstraintValidator<AmountDivisibleByNumberOfDays, InitiatePaymentRequest> {

  /**
   * Checks if the passed {@code initiatePaymentRequest} contains an amount which is divisible by
   * the number of days ({@link InitiatePaymentRequest#getDays()}).
   *
   * @param initiatePaymentRequest A request which is to be validated.
   * @param constraintValidatorContext Validator context (unused).
   * @return true if {@code initiatePaymentRequest}, {@link InitiatePaymentRequest#getDays()} or
   *     {@link InitiatePaymentRequest#getAmount()} are null or the amount is divisible by the
   *     number of days (provided they are positive), false otherwise.
   */
  @Override
  public boolean isValid(InitiatePaymentRequest initiatePaymentRequest,
      ConstraintValidatorContext constraintValidatorContext) {
    if (initiatePaymentRequest == null
        || initiatePaymentRequest.getDays() == null
        || initiatePaymentRequest.getAmount() == null) {
      return true;
    }
    int numberOfDays = initiatePaymentRequest.getDays().size();
    return numberOfDays > 0 && initiatePaymentRequest.getAmount() % numberOfDays == 0;
  }
}
