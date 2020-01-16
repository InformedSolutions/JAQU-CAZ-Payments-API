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
import uk.gov.caz.psr.model.EntrantPaymentMatch;

@ExtendWith(MockitoExtension.class)
class EntrantPaymentMatchRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private EntrantPaymentMatchRepository entrantPaymentMatchRepository;

  @Nested
  class Insert {

    @Nested
    class WhenEntrantPaymentMatchIsNull {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        EntrantPaymentMatch entrantPaymentMatch = null;

        // when
        Throwable throwable = catchThrowable(() -> entrantPaymentMatchRepository.insert(
            entrantPaymentMatch));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("'entrantPaymentMatch' cannot be null");
      }
    }
    @Nested
    class WhenEntrantPaymentMatchIdIsNull {

      @Test
      public void shouldThrowIllegalArgumentException() {
        // given
        EntrantPaymentMatch entrantPaymentMatch = EntrantPaymentMatch.builder()
            .id(UUID.randomUUID())
            .build();

        // when
        Throwable throwable = catchThrowable(() -> entrantPaymentMatchRepository.insert(
            entrantPaymentMatch));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
        assertThat(throwable).hasMessage("'entrantPaymentMatch' cannot have ID");
      }
    }
  }
}