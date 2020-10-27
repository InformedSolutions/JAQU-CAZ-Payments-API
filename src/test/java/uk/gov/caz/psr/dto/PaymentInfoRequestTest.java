package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.controller.exception.PaymentInfoPaymentMadeDateValidationException;

class PaymentInfoRequestTest {

  private static final String ANY_PAYMENT_PROVIDER_ID = "payment-provider";
  private static final String ANY_VRN = "CAS123";
  private static final LocalDate ANY_FROM_PAID_FOR = LocalDate.now().minusDays(10);
  private static final LocalDate ANY_TO_DATE_PAID_FOR = LocalDate.now();
  private static final LocalDate ANY_PAYMENT_MADE_DATE = LocalDate.now();

  @Nested
  class PaymentMadeDateConjunction {

    @Test
    public void shouldThrowExceptionWhenPaymentMadeDateAndPaymentProviderIdArePresent() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .paymentProviderId(ANY_PAYMENT_PROVIDER_ID)
          .paymentMadeDate(ANY_PAYMENT_MADE_DATE)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable)
          .isInstanceOf(PaymentInfoPaymentMadeDateValidationException.class)
          .hasMessage(
              "This parameter cannot be used in conjunction with another other request parameters");
    }

    @Test
    public void shouldThrowExceptionWhenPaymentMadeDateAndVrnArePresent() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .vrn(ANY_VRN)
          .paymentMadeDate(ANY_PAYMENT_MADE_DATE)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable)
          .isInstanceOf(PaymentInfoPaymentMadeDateValidationException.class)
          .hasMessage(
              "This parameter cannot be used in conjunction with another other request parameters");

    }

    @Test
    public void shouldThrowExceptionWhenPaymentMadeDateAndFromDatePaidArePresent() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .fromDatePaidFor(ANY_FROM_PAID_FOR)
          .paymentMadeDate(ANY_PAYMENT_MADE_DATE)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable)
          .isInstanceOf(PaymentInfoPaymentMadeDateValidationException.class)
          .hasMessage(
              "This parameter cannot be used in conjunction with another other request parameters");

    }

    @Test
    public void shouldThrowExceptionWhenPaymentMadeDateAndToPaidDateArePresent() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .toDatePaidFor(ANY_TO_DATE_PAID_FOR)
          .paymentMadeDate(ANY_PAYMENT_MADE_DATE)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable)
          .isInstanceOf(PaymentInfoPaymentMadeDateValidationException.class)
          .hasMessage(
              "This parameter cannot be used in conjunction with another other request parameters");
    }

    @Test
    public void shouldNotThrowExceptionWhenPaymentMadeDateIsNull() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .paymentProviderId(ANY_PAYMENT_PROVIDER_ID)
          .fromDatePaidFor(ANY_FROM_PAID_FOR)
          .toDatePaidFor(ANY_TO_DATE_PAID_FOR)
          .vrn(ANY_VRN)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable).isNull();
    }

    @Test
    public void shouldNotThrowExceptionWhenOnlyPaymentMadeDateIsPresent() {
      // given
      PaymentInfoRequestV1 request = PaymentInfoRequestV1.builder()
          .paymentMadeDate(ANY_PAYMENT_MADE_DATE)
          .build();

      // when
      Throwable throwable = catchThrowable(() -> request.validateParametersConjunction());

      // then
      assertThat(throwable).isNull();
    }
  }
}
