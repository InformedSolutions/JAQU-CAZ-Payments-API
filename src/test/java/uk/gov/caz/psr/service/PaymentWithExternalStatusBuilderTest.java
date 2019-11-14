package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class PaymentWithExternalStatusBuilderTest {

  private PaymentWithExternalStatusBuilder builder = new PaymentWithExternalStatusBuilder();

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;
    ExternalPaymentStatus status = ExternalPaymentStatus.SUCCESS;

    // when
    Throwable throwable = catchThrowable(() -> builder.buildPaymentWithStatus(payment, status));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenStatusIsNull() {
    // given
    Payment payment = anyPayment();
    ExternalPaymentStatus status = null;

    // when
    Throwable throwable = catchThrowable(() -> builder.buildPaymentWithStatus(payment, status));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("newStatus cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenNewStatusIsTheSameAsTheExistingOne() {
    // given
    ExternalPaymentStatus initStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = anyPaymentWithStatus(initStatus);
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.INITIATED;

    // when
    Throwable throwable = catchThrowable(() -> builder.buildPaymentWithStatus(payment, newStatus));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Status cannot be equal to the existing status");
  }

  @Test
  public void shouldCreateNewPaymentWithPassedStatus() {
    // given
    ExternalPaymentStatus initStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = anyPaymentWithStatus(initStatus);
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.CREATED;

    // when
    Payment result = builder.buildPaymentWithStatus(payment, newStatus);

    // then
    assertThat(result.getExternalPaymentStatus()).isEqualTo(newStatus);
    assertThat(result.getAuthorisedTimestamp()).isNull();
    assertThat(result.getVehicleEntrantPayments()).allSatisfy((vehicleEntrantPayment -> {
      assertThat(vehicleEntrantPayment.getInternalPaymentStatus())
          .isEqualTo(InternalPaymentStatus.NOT_PAID);
    }));
  }

  @Test
  public void shouldCreateNewSuccessPaymentWithAuthTimestampSetAndPaidInternalStatus() {
    // given
    ExternalPaymentStatus initStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = anyPaymentWithStatus(initStatus);
    ExternalPaymentStatus newStatus = ExternalPaymentStatus.SUCCESS;

    // when
    Payment result = builder.buildPaymentWithStatus(payment, newStatus);

    // then
    assertThat(result.getExternalPaymentStatus()).isEqualTo(newStatus);
    assertThat(result.getAuthorisedTimestamp()).isNotNull();
    assertThat(result.getVehicleEntrantPayments()).allSatisfy((vehicleEntrantPayment -> {
      assertThat(vehicleEntrantPayment.getInternalPaymentStatus())
          .isEqualTo(InternalPaymentStatus.PAID);
    }));
  }

  private Payment anyPaymentWithStatus(ExternalPaymentStatus status) {
    return TestObjectFactory.Payments.existing()
        .toBuilder()
        .externalPaymentStatus(status)
        .build();
  }

  private Payment anyPayment() {
    return TestObjectFactory.Payments.existing();
  }
}