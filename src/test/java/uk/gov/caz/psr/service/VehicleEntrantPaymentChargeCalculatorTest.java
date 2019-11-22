package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantPaymentChargeCalculatorTest {

  private final VehicleEntrantPaymentChargeCalculator chargeCalculator =
      new VehicleEntrantPaymentChargeCalculator();

  @ParameterizedTest
  @ValueSource(ints = {-12, -9124, -10, -1, 0})
  public void shouldThrowIllegalArgumentExceptionWhenNumberOfDaysIsNotPositive(int numberOfDays) {
    // given
    int total = 4200;

    // when
    Throwable throwable = catchThrowable(() ->
        chargeCalculator.calculateCharge(total, numberOfDays));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Expecting 'numberOfDays'");
  }

  @ParameterizedTest
  @ValueSource(ints = {-3, -11, -82414, 0})
  public void shouldThrowIllegalArgumentExceptionWhenTotalIsNotPositive(int total) {
    // given
    int numberOfDays = 6;

    // when
    Throwable throwable = catchThrowable(() ->
        chargeCalculator.calculateCharge(total, numberOfDays));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Expecting 'total'");
  }

  @ParameterizedTest
  @MethodSource("totalDivisibleByNumberOfDays")
  public void shouldReturnTheAmountDividedByTheNumberOfDays(int total, int numberOfDays) {
    // given

    // when
    int result = chargeCalculator.calculateCharge(total, numberOfDays);

    // then
    int expected = total / numberOfDays;
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void shouldAcceptTotalNotDivisibleByNumberOfDays() {
    // given
    int total = 24;
    int numberOfDays = 5;

    // when
    int result = chargeCalculator.calculateCharge(total, numberOfDays);

    // then
    assertThat(result).isEqualTo(4);
  }

  static Stream<Arguments> totalDivisibleByNumberOfDays() {
    return Stream.of(
        Arguments.arguments(10, 5),
        Arguments.arguments(24, 8),
        Arguments.arguments(80, 10)
    );
  }
}