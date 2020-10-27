package uk.gov.caz.psr.dto.validation.constraint;

import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import uk.gov.caz.psr.dto.PaymentInfoRequestV1;

/**
 * Custom validator that checks whether the request contains from and to dates in chronological
 * order.
 */
public class FromAndToDatesInChronologicalOrderValidator implements
    ConstraintValidator<FromAndToDatesInChronologicalOrder, PaymentInfoRequestV1> {

  /**
   * Checks if the passed {@code input} contains from and to dates in chronological order.
   *
   * @param input An input request which will be validated.
   * @param context Validator context (unused).
   * @return true if {@code input} contains from and to dates in chronological order.
   */
  @Override
  public boolean isValid(PaymentInfoRequestV1 input, ConstraintValidatorContext context) {
    if (input == null) {
      return true;
    }
    LocalDate fromDatePaidFor = input.getFromDatePaidFor();
    LocalDate toDatePaidFor = input.getToDatePaidFor();
    return fromDatePaidFor == null
        || toDatePaidFor == null
        || isBeforeOrEqualTo(fromDatePaidFor, toDatePaidFor);
  }

  /**
   * Returns {@code true} if {@code toDatePaidFor} is greater than or equal to {@code
   * fromDatePaidFor}, false otherwise.
   */
  private boolean isBeforeOrEqualTo(LocalDate fromDatePaidFor, LocalDate toDatePaidFor) {
    // fromDatePaidFor <= toDatePaidFor  <=> !(fromDatePaidFor > toDatePaidFor)
    return !fromDatePaidFor.isAfter(toDatePaidFor);
  }
}
