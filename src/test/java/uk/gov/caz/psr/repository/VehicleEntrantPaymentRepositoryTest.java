package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantPaymentRepositoryTest {
  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenInsertListIsNull() {
      // given
      List<VehicleEntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle entrant payments cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsEmpty() {
      // given
      List<VehicleEntrantPayment> input = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments cannot be empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsContainsNullPaymentId() {
      // given
      List<VehicleEntrantPayment> input = Payments.forRandomDays().getVehicleEntrantPayments();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Each vehicle entrant payment must have 'payment_id' set");
    }
  }

  @Nested
  class Update {

    @Test
    public void shouldThrowNullPointerExceptionWhenListIsNull() {
      // given
      List<VehicleEntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.update(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle entrant payments cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenListIsEmpty() {
      // given
      List<VehicleEntrantPayment> input = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.update(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments cannot be empty");
    }
  }
}