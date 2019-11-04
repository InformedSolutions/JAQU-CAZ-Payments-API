package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.Payment;
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
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasId() {
      // given
      Payment payment = Payments.forRandomDaysWithId(
          UUID.fromString("c70d7c3c-fbb3-11e9-a4bd-4308a048c150")
      );

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Payment cannot have ID");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasEmptyVehicleEntrantPayments() {
      // given
      Payment payment = Payments.forRandomDays()
          .toBuilder()
          .vehicleEntrantPayments(Collections.emptyList())
          .build();

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments cannot be empty");
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