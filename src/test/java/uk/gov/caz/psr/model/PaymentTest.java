package uk.gov.caz.psr.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment.PaymentBuilder;

@ExtendWith(MockitoExtension.class)
class PaymentTest {

  @Nested
  class CreatingWithExternalId {

    @Nested
    class WhenExternalIdIsNotNull {

      @Test
      public void shouldThrowIllegalStateExceptionWhenExtPaymentStatusIsNull() {
        // given
        String externalId = "ext-id";
        ExternalPaymentStatus status = null;

        // when
        Throwable throwable = catchThrowable(() ->
            Payment.builder()
                .externalId(externalId)
                .externalPaymentStatus(status)
                .build()
        );

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Illegal values of external payment status and ext id");
      }
    }

    @Nested
    class WhenExternalIdIsNull {

      @Test
      public void shouldThrowIllegalStateExceptionWhenExtPaymentStatusIsNotNullAndNotInit() {
        // given
        ExternalPaymentStatus status = ExternalPaymentStatus.STARTED;

        // when
        Throwable throwable = catchThrowable(() ->
            Payment.builder()
                .externalId(null)
                .externalPaymentStatus(status)
                .build()
        );

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Illegal values of external payment status and ext id");
      }
    }
  }
  @Nested
  class CreatingWithAuthorisedTimestamp {

    @Nested
    class WhenAuthorisedTimestampIsNull {
      @Test
      public void shouldThrowIllegalStateExceptionWhenExtPaymentStatusIsSuccess() {
        // given
        String externalId = "ext-id";
        ExternalPaymentStatus status = ExternalPaymentStatus.SUCCESS;

        // when
        Throwable throwable = catchThrowable(() ->
            Payment.builder()
                .externalId(externalId)
                .externalPaymentStatus(status)
                .authorisedTimestamp(null)
                .build()
        );

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessage("authorisedTimestamp is null and external payment status is not 'SUCCESS' "
                + "or authorisedTimestamp is not null and external payment status is 'SUCCESS'");
      }
    }

    @Nested
    class WhenAuthorisedTimestampIsNotNull {
      @Test
      public void shouldThrowIllegalStateExceptionWhenExtPaymentStatusIsNotSuccess() {
        // given
        String externalId = "ext-id";
        LocalDateTime authorisedTimestamp = LocalDateTime.now();
        ExternalPaymentStatus status = ExternalPaymentStatus.INITIATED;

        // when
        Throwable throwable = catchThrowable(() ->
            Payment.builder()
                .externalId(externalId)
                .externalPaymentStatus(status)
                .authorisedTimestamp(authorisedTimestamp)
                .build()
        );

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessage("authorisedTimestamp is null and external payment status is not 'SUCCESS' "
                + "or authorisedTimestamp is not null and external payment status is 'SUCCESS'");
      }
    }
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentMethodIsNull() {
    // given
    PaymentMethod paymentMethod = null;

    // when
    Throwable throwable = catchThrowable(() ->
        Payment.builder().paymentMethod(paymentMethod).build());

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("paymentMethod");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenTotalPaidIsNull() {
    // given
    Integer totalPaid = null;

    // when
    Throwable throwable = catchThrowable(() ->
        Payment.builder()
            .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
            .totalPaid(totalPaid)
            .build()
    );

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("totalPaid");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenEntrantPaymentsIsNull() {
    // given
    List<EntrantPayment> entrantPayments = null;

    // when
    Throwable throwable = catchThrowable(() ->
        Payment.builder()
            .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
            .totalPaid(100)
            .entrantPayments(entrantPayments)
            .build()
    );

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("entrantPayments");
  }

  @Test
  public void shouldCreatePaymentInstanceWithNullExtIdAndNullExtStatus() {
    PaymentBuilder paymentBuilder = Payment.builder()
        .externalId(null)
        .externalPaymentStatus(null)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .totalPaid(15)
        .entrantPayments(Collections.emptyList());

    assertThatCode(paymentBuilder::build).doesNotThrowAnyException();
  }
}