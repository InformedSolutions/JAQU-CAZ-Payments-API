package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
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
      Throwable throwable =
          catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPaymentHasId() {
      // given
      Payment payment = Payments.forRandomDaysWithId(
          UUID.fromString("c70d7c3c-fbb3-11e9-a4bd-4308a048c150"), null);

      // when
      Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Payment cannot have ID");
    }

    @Nested
    class WhenPaymentHasFalseTelephonePaymentAndValidOperatorId {

      @Test
      public void shouldThrowIllegalArgumentException() {
        // given
        Payment payment = Payments.forRandomDays().toBuilder()
            .entrantPayments(Collections.emptyList())
            .telephonePayment(false)
            .operatorId(UUID.randomUUID())
            .build();

        // when
        Throwable throwable = catchThrowable(() -> paymentRepository.insert(payment));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operator ID must be null if telephonePayment is false");
      }
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

  @Nested
  class FindByEntrantPayment {

    @Test
    public void shouldThrowIllegalStateExceptionWhenMoreThanOneRowIsReturned() {
      UUID entrantPaymentId = UUID.fromString("b49ee61e-b9c3-497a-b729-e924c3f57930");
      mockMoreThanOneRowReturned();

      Throwable throwable = catchThrowable(() ->
          paymentRepository.findByEntrantPayment(entrantPaymentId));

      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessageStartingWith("Found more than one payments for entrant payment id");
    }

    private void mockMoreThanOneRowReturned() {
      when(jdbcTemplate.query(any(String.class), any(PreparedStatementSetter.class),
          any(RowMapper.class))
      ).thenReturn(Arrays.asList(Payments.forRandomDays(), Payments.forRandomDays()));
    }
  }

  @Nested
  class MarkSentConfirmationEmail {

    @Test
    public void shouldMarkTheProvidedPaymentAsConfirmationEmailSent() {
      UUID paymentId = null;

      Throwable throwable = catchThrowable(
          () -> paymentRepository.markSentConfirmationEmail(paymentId));

      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("paymentId cannot be null");
    }
  }
}
