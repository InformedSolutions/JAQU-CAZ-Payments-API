package uk.gov.caz.psr.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class PaymentTest {

  @Nested
  class CreatingWithStatus {
    @Test
    public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentsIsNull() {
      // when
      Throwable throwable = catchThrowable(() ->
          Payment.builder().status(PaymentStatus.INITIATED));

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("Vehicle entrant payments are empty or do not have one common status");
    }
  }

  @Nested
  class GetStatus {
    @Test
    public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentsIsEmpty() {
      // given
      Payment payment = Payments.forRandomDays()
          .toBuilder()
          .vehicleEntrantPayments(Collections.emptyList())
          .build();

      // when
      Throwable throwable = catchThrowable(payment::getStatus);

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("Vehicle entrant payments are empty or do not have one common status");
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentsContainDifferentStatuses() {
      // given
      Payment payment = paymentWithTwoDifferentStatuses();

      // when
      Throwable throwable = catchThrowable(payment::getStatus);

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("Vehicle entrant payments are empty or do not have one common status");
    }

    private Payment paymentWithTwoDifferentStatuses() {
      Payment payment = Payments.forDays(Arrays.asList(LocalDate.now(),
          LocalDate.now().plusDays(1)), UUID.randomUUID());
      Iterator<VehicleEntrantPayment> it = payment.getVehicleEntrantPayments().iterator();
      List<VehicleEntrantPayment> updatedVehicleEntrantPayments = Arrays
          .asList(PaymentStatus.SUCCESS, PaymentStatus.FAILED)
          .stream()
          .map(status -> it.next().toBuilder().status(status).build())
          .collect(Collectors.toList());

      return payment.toBuilder()
          .vehicleEntrantPayments(updatedVehicleEntrantPayments)
          .build();
    }
  }
}