package uk.gov.caz.psr.dto.validation.constraint;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Custom constraint annotation for checking whether the amount is divisible by the number of days.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = AmountDivisibleByNumberOfDaysValidator.class)
@Documented
public @interface AmountDivisibleByNumberOfDays {
  /**
   * A message that will be returned when validation fails.
   */
  String message() default "amount / daysCount is not a natural number";

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<?>[] groups() default { };

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<? extends Payload>[] payload() default { };
}
