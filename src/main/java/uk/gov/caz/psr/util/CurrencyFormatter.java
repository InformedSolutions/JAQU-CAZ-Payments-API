package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;

@Component
public class CurrencyFormatter {

  public double parsePennies(int totalAmount) {
    return (double) totalAmount / 100;
  }
}
