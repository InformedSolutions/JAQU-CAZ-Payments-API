package uk.gov.caz.psr.dto.validation.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;

@ExtendWith(MockitoExtension.class)
class AmountDivisibleByNumberOfDaysValidatorTest {

  private static final List<LocalDate> DAYS = Arrays.asList(
      LocalDate.of(2019, 1, 1),
      LocalDate.of(2019, 1, 3)
  );

  private AmountDivisibleByNumberOfDaysValidator validator =
      new AmountDivisibleByNumberOfDaysValidator();

  @Test
  public void shouldReturnTrueIfRequestIsNull() {
    // given
    InitiatePaymentRequest request = null;

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueIfDaysAreNull() {
    // given
    InitiatePaymentRequest request = baseRequestBuilder().days(null).build();

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueIfAmountIsNull() {
    // given
    InitiatePaymentRequest request = baseRequestBuilder().amount(null).build();

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnFalseIfDaysAreEmpty() {
    // given
    InitiatePaymentRequest request = baseRequestBuilder().days(Collections.emptyList()).build();

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 20, 2, 48, 64})
  public void shouldReturnTrueIfAmountIsDivisible(int amount) {
    // given
    InitiatePaymentRequest request = baseRequestBuilder().amount(amount).build();

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {101, 25, 3, 49, 67})
  public void shouldReturnFalseIfAmountIsNotDivisible(int amount) {
    // given
    InitiatePaymentRequest request = baseRequestBuilder().amount(amount).build();

    // when
    boolean result = validator.isValid(request, null);

    // then
    assertThat(result).isFalse();
  }


  private InitiatePaymentRequest.InitiatePaymentRequestBuilder baseRequestBuilder() {
    return InitiatePaymentRequest.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .days(DAYS)
        .vrn("TEST123")
        .amount(1050)
        .returnUrl("https://example.return.url");
  }
}