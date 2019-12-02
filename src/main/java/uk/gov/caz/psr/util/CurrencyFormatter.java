package uk.gov.caz.psr.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Utility class to format values of pennies into pounds.
 */
@Component
public class CurrencyFormatter {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  /**
   * Method to convert pennies into pounds.
   * 
   * @param totalAmount the amount in pennies
   * @return the amount in pounds
   */
  public double parsePennies(int totalAmount) {
    return (double) totalAmount / 100;
  }

  /**
   * Converts {@code totalAmount} into pounds represented by {@link BigDecimal}.
   * @param totalAmount The amount to be converted in pennies
   * @return the amount in pounds
   */
  public BigDecimal parsePenniesToBigDecimal(int totalAmount) {
    return BigDecimal.valueOf(totalAmount).divide(ONE_HUNDRED, 2, RoundingMode.DOWN);
  }
}
