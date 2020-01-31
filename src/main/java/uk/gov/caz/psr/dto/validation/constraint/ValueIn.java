package uk.gov.caz.psr.dto.validation.constraint;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Constraint annotation for checking whether the value from the request has value from provided
 * array using {@code ValueInValidator}.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValueInValidator.class)
@Documented
public @interface ValueIn {

  /**
   * All possible values of Enum.
   */
  String[] possibleValues();

  /**
   * A message that will be returned when validation fails.
   */
  String message() default "Value is not valid";

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<?>[] groups() default {};

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<? extends Payload>[] payload() default {};
}
