package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.ExternalPaymentDetailsFactory;

@ExtendWith(MockitoExtension.class)
class PaymentUpdateStatusBuilderTest {

  private PaymentUpdateStatusBuilder builder = new PaymentUpdateStatusBuilder();

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;
    ExternalPaymentDetails externalPaymentDetails = ExternalPaymentDetailsFactory.any();

    // when
    Throwable throwable = catchThrowable(
        () -> builder.buildWithExternalPaymentDetails(payment, externalPaymentDetails));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenStatusIsNull() {
    // given
    Payment payment = anyPayment();
    ExternalPaymentDetails externalPaymentDetails = null;

    // when
    Throwable throwable = catchThrowable(
        () -> builder.buildWithExternalPaymentDetails(payment, externalPaymentDetails));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("externalPaymentDetails cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenNewStatusIsTheSameAsTheExistingOne() {
    // given
    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);

    // when
    Throwable throwable = catchThrowable(
        () -> builder.buildWithExternalPaymentDetails(payment, newExternalPaymentDetails));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Status cannot be equal to the existing status");
  }

  @Test
  public void shouldCreateNewPaymentObjectWithPassedStatus() {
    // given
    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.CREATED);

    // when
    Payment result = builder
        .buildWithExternalPaymentDetails(payment, newExternalPaymentDetails);

    // then
    assertThat(result.getExternalPaymentStatus())
        .isEqualTo(newExternalPaymentDetails.getExternalPaymentStatus());
    assertThat(result.getAuthorisedTimestamp()).isNull();
    assertThat(result.getEntrantPayments()).allSatisfy((entrantPayment -> {
      assertThat(entrantPayment.getInternalPaymentStatus())
          .isEqualTo(InternalPaymentStatus.NOT_PAID);
    }));
  }

  @Test
  public void shouldReturnEmptyListOfEntrantPaymentWhenStatusNotPaid() {
    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.CREATED);

    // when
    Payment result = builder
        .buildWithExternalPaymentDetails(payment, newExternalPaymentDetails);

    // then
    assertThat(payment.getEntrantPayments()).isNotEmpty();
    assertThat(result.getEntrantPayments()).isEmpty();
  }


  @Test
  public void shouldReturnUpdatedListOfEntrantPaymentWhenStatusPaid() {
    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.SUCCESS);

    // when
    Payment result = builder
        .buildWithExternalPaymentDetails(payment, newExternalPaymentDetails);

    // then
    assertThat(result.getEntrantPayments()).isNotEmpty();
  }

  @Test
  public void shouldCreateNewSuccessPaymentWithAuthTimestampSetAndPaidInternalStatus() {
    // given
    ExternalPaymentDetails initExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.INITIATED);
    Payment payment = anyPaymentWithStatus(initExternalPaymentDetails.getExternalPaymentStatus());
    ExternalPaymentDetails newExternalPaymentDetails = ExternalPaymentDetailsFactory
        .anyWithStatus(ExternalPaymentStatus.SUCCESS);

    // when
    Payment result = builder
        .buildWithExternalPaymentDetails(payment, newExternalPaymentDetails);

    // then
    assertThat(result.getExternalPaymentStatus())
        .isEqualTo(newExternalPaymentDetails.getExternalPaymentStatus());
    assertThat(result.getAuthorisedTimestamp()).isNotNull();
    assertThat(result.getEntrantPayments()).allSatisfy((entrantPayment -> {
      assertThat(entrantPayment.getInternalPaymentStatus())
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