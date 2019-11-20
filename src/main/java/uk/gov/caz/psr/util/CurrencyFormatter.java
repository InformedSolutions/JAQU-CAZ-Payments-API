package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;

/**
 * Utility class to format values of pennies into pounds.
 */
@Component
public class CurrencyFormatter {

  /**
   * Method to convert pennies into pounds.
   * 
   * @param totalAmount the amount in pennies
   * @return the amount in pounds
   */
  public double parsePennies(int totalAmount) {
    return (double) totalAmount / 100;
  }
}
