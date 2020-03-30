package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class CurrencyFormatterTest {
  private CurrencyFormatter currencyFormatter = new CurrencyFormatter();

  @ParameterizedTest
  @CsvSource({"1,0.01", "100,1", "250,2.50", "2439, 24.39"})
  void canParsePennies(String input, String expected) {
    // given
    int totalAmount = Integer.parseInt(input);

    // when
    double totalAmountInPounds = currencyFormatter.parsePennies(totalAmount);

    // then
    double expectedAmount = Double.parseDouble(expected);
    assertEquals(expectedAmount, totalAmountInPounds);
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.psr.util.CurrencyFormatterTest#conversionToBigDecimal")
  void canParsePenniesToBigDecimal(int input, BigDecimal expected) {
    // when
    BigDecimal result = currencyFormatter.parsePenniesToBigDecimal(input);

    // then
    assertThat(result).isEqualTo(expected);
  }

  public static Stream<Arguments> conversionToBigDecimal() {
    return Stream.of(
      Arguments.of(1, new BigDecimal("0.01")),
      Arguments.of(100, new BigDecimal("1.00")),
      Arguments.of(250, new BigDecimal("2.50")),
      Arguments.of(3712, new BigDecimal("37.12")),
      Arguments.of(378, new BigDecimal("3.78"))
    );
  }
}
