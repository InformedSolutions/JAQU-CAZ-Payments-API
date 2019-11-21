package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class PaymentStatusRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PaymentStatusRepository paymentStatusRepository;

  @Nested
  class FindBy {
    @Test
    public void shouldThrowNullPointerExceptionWhenDateOfCazEntryIsNull() {
      LocalDate dateOfCazEntry = null;
      String vrn = "CAS123";
      UUID cazId = UUID.randomUUID();

      Throwable throwable = catchThrowable(
          () -> paymentStatusRepository.findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry));

      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("dateOfCazEntry cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenVrnIsNull() {
      LocalDate dateOfCazEntry = LocalDate.now();
      String vrn = null;
      UUID cazId = UUID.randomUUID();

      Throwable throwable = catchThrowable(
          () -> paymentStatusRepository.findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry));

      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("VRN cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenCazIdIsNull() {
      LocalDate dateOfCazEntry = LocalDate.now();
      String vrn = "CAS123";
      UUID cazId = null;

      Throwable throwable = catchThrowable(
          () -> paymentStatusRepository.findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry));

      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("CAZ ID cannot be null");
    }
  }
}
