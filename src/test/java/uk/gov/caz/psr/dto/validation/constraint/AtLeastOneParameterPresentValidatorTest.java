package uk.gov.caz.psr.dto.validation.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.PaymentInfoRequest;

@ExtendWith(MockitoExtension.class)
class AtLeastOneParameterPresentValidatorTest {

  private static final String ANY_VRN = "ZC62OMB";
  private static final String ANY_PAYMENT_ID = "payment-id";
  private static final LocalDate ANY_DATE = LocalDate.now();

  @Mock
  private ConstraintValidatorContext context;

  private AtLeastOneParameterPresentValidator validator = new AtLeastOneParameterPresentValidator();

  @Test
  public void shouldReturnTrueWhenInputIsNull() {
    // given
    PaymentInfoRequest request = null;

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnFalseWhenAllParametersAreNull() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder().build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isFalse();
  }

  @Test
  public void shouldReturnTrueWhenToDatePaidForIsPresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .toDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenFromDatePaidForIsPresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .fromDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenBothDatesArePresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .toDatePaidFor(ANY_DATE)
        .fromDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenVrnIsPresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .vrn(ANY_VRN)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenVrnAndToDatePaidArePresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .vrn(ANY_VRN)
        .toDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenVrnAndFromDatePaidArePresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .vrn(ANY_VRN)
        .fromDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenVrnAndDatesArePresent() {
    // given
    PaymentInfoRequest request = PaymentInfoRequest.builder()
        .vrn(ANY_VRN)
        .fromDatePaidFor(ANY_DATE)
        .toDatePaidFor(ANY_DATE)
        .build();

    // when
    boolean result = validator.isValid(request, context);

    // then
    assertThat(result).isTrue();
  }

  @Nested
  class WithPaymentId {
    @Test
    public void shouldReturnTrueWhenToDatePaidForIsPresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .toDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenFromDatePaidForIsPresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .fromDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenBothDatesArePresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .toDatePaidFor(ANY_DATE)
          .fromDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenVrnIsPresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .vrn(ANY_VRN)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenVrnAndToDatePaidArePresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .vrn(ANY_VRN)
          .toDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenVrnAndFromDatePaidArePresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .vrn(ANY_VRN)
          .fromDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenVrnAndDatesArePresent() {
      // given
      PaymentInfoRequest request = PaymentInfoRequest.builder()
          .paymentProviderId(ANY_PAYMENT_ID)
          .vrn(ANY_VRN)
          .fromDatePaidFor(ANY_DATE)
          .toDatePaidFor(ANY_DATE)
          .build();

      // when
      boolean result = validator.isValid(request, context);

      // then
      assertThat(result).isTrue();
    }
  }
}