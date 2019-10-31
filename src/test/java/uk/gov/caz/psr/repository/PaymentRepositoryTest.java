package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

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
      Payment payment = Payment.builder()
          .id(UUID.fromString("c70d7c3c-fbb3-11e9-a4bd-4308a048c150"))
          .correlationId("1f9d3f20-fbb6-11e9-8965-5f4601819905")
          .paymentMethod(PaymentMethod.CREDIT_CARD)
          .status(PaymentStatus.INITIATED).chargePaid(10).build();

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Payment cannot have ID");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenCorrelationIdIsNull() {
      // given
      Payment payment = Payment.builder().paymentMethod(PaymentMethod.CREDIT_CARD)
          .status(PaymentStatus.INITIATED).chargePaid(10).build();

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Correlation ID cannot be null");
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