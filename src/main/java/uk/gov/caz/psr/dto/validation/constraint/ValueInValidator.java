package uk.gov.caz.psr.dto.validation.constraint;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator that checks whether the passed argument has the value from provided valid values
 * array.
 */
public class ValueInValidator implements
    ConstraintValidator<ValueIn, String> {

  private Set<String> validValues;

  @Override
  public void initialize(ValueIn constraint) {
    validValues = Arrays.stream(constraint.possibleValues()).collect(toSet());
  }

  /**
   * Checks if the passed {@code input} contains at least on non-null parameter.
   *
   * @param input An input request which will be validated.
   * @param context Validator context (unused).
   * @return true if {@code input} contains at least on non-null parameter, false otherwise.
   */
  @Override
  public boolean isValid(String input, ConstraintValidatorContext context) {
    return validValues.contains(input);
  }
}
