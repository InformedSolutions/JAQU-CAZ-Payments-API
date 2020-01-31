package uk.gov.caz.psr.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentStatusTest {

  private static final EnumSet<ExternalPaymentStatus> FINISHED_STATUSES = EnumSet.of(
      ExternalPaymentStatus.FAILED,
      ExternalPaymentStatus.SUCCESS,
      ExternalPaymentStatus.CANCELLED,
      ExternalPaymentStatus.ERROR
  );

  @Test
  public void testFinishedStatuses() {
    // given
    EnumSet<ExternalPaymentStatus> input = FINISHED_STATUSES;

    // when
    boolean result = input.stream().noneMatch(ExternalPaymentStatus::isNotFinished);

    // then
    assertThat(result).isTrue();
  }

  @Test
  public void testNotFinishedStatuses() {
    // given
    EnumSet<ExternalPaymentStatus> input = EnumSet.complementOf(FINISHED_STATUSES);

    // when
    boolean result = input.stream().allMatch(ExternalPaymentStatus::isNotFinished);

    // then
    assertThat(result).isTrue();
  }
}