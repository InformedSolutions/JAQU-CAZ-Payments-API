package uk.gov.caz.psr.repository;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PaymentRepository paymentRepository;

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
      // given
      Payment payment = null;

      // when
      Throwable throwable =
          catchThrowable(() -> paymentRepository.insertWithExternalStatus(payment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasId() {
      // given
      Payment payment =
          Payments.forRandomDaysWithId(UUID.fromString("c70d7c3c-fbb3-11e9-a4bd-4308a048c150"), null);

      // when
      Throwable throwable =
          catchThrowable(() -> paymentRepository.insertWithExternalStatus(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Payment cannot have ID");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrantPayments() {
      // given
      Payment payment = paymentWithEmptyVehicleEntrantPayments();

      // when
      Throwable throwable =
          catchThrowable(() -> paymentRepository.insertWithExternalStatus(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments cannot be empty");
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenVehicleEntrantPaymentsContainDifferentStatuses() {
      // given
      Payment payment = paymentWithTwoDifferentVehicleEntrantStatuses();

      // when
      Throwable throwable =
          catchThrowable(() -> paymentRepository.insertWithExternalStatus(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments do not have one common status");
    }

    private Payment paymentWithEmptyVehicleEntrantPayments() {
      return Payments.forRandomDays().toBuilder().vehicleEntrantPayments(Collections.emptyList())
          .build();
    }

    private Payment paymentWithTwoDifferentVehicleEntrantStatuses() {
      Payment payment =
          Payments.forDays(Arrays.asList(LocalDate.now(), LocalDate.now().plusDays(1)),
              null, null, null);
      Iterator<VehicleEntrantPayment> it = payment.getVehicleEntrantPayments().iterator();
      List<VehicleEntrantPayment> updatedVehicleEntrantPayments =
          Arrays.asList(InternalPaymentStatus.PAID, InternalPaymentStatus.CHARGEBACK).stream()
              .map(status -> it.next().toBuilder().internalPaymentStatus(status).build())
              .collect(Collectors.toList());

      return payment.toBuilder().id(null).vehicleEntrantPayments(updatedVehicleEntrantPayments)
          .build();
    }
  }

  @Nested
  class Update {
    @Test
    public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
      // given
      Payment payment = null;

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.update(payment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment cannot be null");
    }
  }

  @Nested
  class FindById {
    @Test
    public void shouldThrowNullPointerExceptionWhenIdIsNull() {
      // given
      UUID id = null;

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.findById(id));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("ID cannot be null");
    }
  }
}
