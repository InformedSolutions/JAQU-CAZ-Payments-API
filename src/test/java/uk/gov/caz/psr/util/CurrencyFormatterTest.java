package uk.gov.caz.psr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CurrencyFormatterTest {

  @ParameterizedTest
  @CsvSource({"1,0.01", "100,1", "250,2.50", "2439, 24.39"})
  void canParsePennies(String input, String expected) {
    CurrencyFormatter currencyFormatter = new CurrencyFormatter();
    int totalAmount = Integer.parseInt(input);
    double expectedAmount = Double.parseDouble(expected);

    double totalAmountInPounds = currencyFormatter.parsePennies(totalAmount);

    assertEquals(expectedAmount, totalAmountInPounds);

  }

}
