package uk.gov.caz.psr.dto.validation.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.PaymentInfoRequestV1;

@ExtendWith(MockitoExtension.class)
class FromAndToDatesInChronologicalOrderValidatorTest {

  @Mock
  private ConstraintValidatorContext context;

  private FromAndToDatesInChronologicalOrderValidator validator =
      new FromAndToDatesInChronologicalOrderValidator();

  @Test
  public void shouldReturnTrueWhenInputIsNull() {
    // given
    PaymentInfoRequestV1 request = null;

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenFromDateIsNull() {
    // given
    PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
        .toDatePaidFor(LocalDate.now())
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenToDateIsNull() {
    // given
    PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
        .fromDatePaidFor(LocalDate.now())
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenFromAndToDatesAreEqual() {
    // given
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = LocalDate.now();
    PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
        .fromDatePaidFor(fromDate)
        .toDatePaidFor(toDate)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenToDateIsAfterFrom() {
    // given
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = fromDate.plusDays(1);
    PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
        .fromDatePaidFor(fromDate)
        .toDatePaidFor(toDate)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnFalseWhenToDateIsBeforeFrom() {
    // given
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = fromDate.minusDays(1);
    PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
        .fromDatePaidFor(fromDate)
        .toDatePaidFor(toDate)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isFalse();
  }
}