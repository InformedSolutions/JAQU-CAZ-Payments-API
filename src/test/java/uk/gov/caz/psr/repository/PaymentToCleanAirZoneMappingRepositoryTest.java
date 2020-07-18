package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentToCleanAirZoneMappingRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PaymentToCleanAirZoneMappingRepository paymentToCleanAirZoneMappingRepository;

  @Nested
  class GetPaymentToCleanAirZoneMapping {

    @Test
    public void shouldThrowNullPointerExceptionWhenUserIdsIsNull() {
      // given
      List<UUID> userIds = null;

      // when
      Throwable throwable = catchThrowable(
          () -> paymentToCleanAirZoneMappingRepository.getPaymentToCleanAirZoneMapping(userIds));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("userIds cannot be null.");
    }
  }
}
