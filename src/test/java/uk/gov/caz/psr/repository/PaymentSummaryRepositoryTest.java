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
class PaymentSummaryRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PaymentSummaryRepository paymentSummaryRepository;

  @Nested
  class GetPaginatedPaymentSummaryForUserIds {

    @Test
    public void shouldThrowNullPointerExceptionWhenUserIdsIsNull() {
      // given
      List<UUID> userIds = null;
      int pageNumber = 0;
      int pageSize = 10;

      // when
      Throwable throwable = catchThrowable(() -> paymentSummaryRepository
          .getPaginatedPaymentSummaryForUserIds(userIds, pageNumber, pageSize));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("userIds cannot be null.");
    }
  }

  @Nested
  class GetTotalPaymentsCountForUserIds {

    @Test
    public void shouldThrowNullPointerExceptionWhenUserIdsIsNull() {
      // given
      List<UUID> userIds = null;

      // when
      Throwable throwable = catchThrowable(() -> paymentSummaryRepository
          .getTotalPaymentsCountForUserIds(userIds));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("userIds cannot be null.");
    }
  }
}
