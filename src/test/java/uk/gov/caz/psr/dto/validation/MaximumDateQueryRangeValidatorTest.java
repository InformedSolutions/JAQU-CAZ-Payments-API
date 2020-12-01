package uk.gov.caz.psr.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.PaymentInfoRequestV1;

@ExtendWith(MockitoExtension.class)
class MaximumDateQueryRangeValidatorTest {

  MaximumDateQueryRangeValidator validator = new MaximumDateQueryRangeValidator(14);

  @ParameterizedTest
  @ValueSource(ints = {14, 15, 22, 33, 111, 333, 643, 712})
  public void shouldThrowExceptionWhenDateRangeIsExceeded(int days) {
    // given
    PaymentInfoRequestV1 paymentInfoRequest = preparePaymentInfoRequestV1(days);

    // when
    Throwable throwable = catchThrowable(() -> validator.validateDateRange(paymentInfoRequest));

    // then
    assertThat(throwable).hasMessage("max date query range exceeded");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 13})
  public void shouldNotThrowExceptionWhenDateRangeIsNotExceeded(int days) {
    // given
    PaymentInfoRequestV1 paymentInfoRequest = preparePaymentInfoRequestV1(days);

    // when
    Throwable throwable = catchThrowable(() -> validator.validateDateRange(paymentInfoRequest));

    // then
    assertThat(throwable).isNull();
  }

  private PaymentInfoRequestV1 preparePaymentInfoRequestV1(int days) {
    return PaymentInfoRequestV1
        .builder()
        .fromDatePaidFor(LocalDate.now())
        .toDatePaidFor(LocalDate.now().plusDays(days))
        .build();
  }
}