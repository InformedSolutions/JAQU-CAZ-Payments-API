package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

public class PaymentHistoryLambdaInputTest {

  private static final UUID ANY_CORRELATION_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final Integer ANY_REGISTER_JOB_ID = RandomUtils.nextInt();
  private static final List<UUID> ANY_ACCOUNT_USER_IDS =
      Collections.singletonList(UUID.randomUUID());

  @Test
  public void shouldCreateValidObject() {
    //given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();

    //when
    lambdaInput.validate();

    // then
    assertThat(lambdaInput.getCorrelationId()).isEqualTo(ANY_CORRELATION_ID);
    assertThat(lambdaInput.getRegisterJobId()).isEqualTo(ANY_REGISTER_JOB_ID);
    assertThat(lambdaInput.getAccountId()).isEqualTo(ANY_ACCOUNT_ID);
    assertThat(lambdaInput.getAccountUserIds()).isEqualTo(ANY_ACCOUNT_USER_IDS);
  }

  @Test
  public void shouldNotAcceptNullCorrelationId() {
    // given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(null)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("correlationId has to be set");
  }

  @Test
  public void shouldNotAcceptNullRegisterJobId() {
    // given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(null)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("registerJobId has to be set");
  }

  @Test
  public void shouldNotAcceptNullAccountId() {
    // given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(null)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("accountId has to be set");
  }

  @Test
  public void shouldNotAcceptNullAccountUserIds() {
    // given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(null)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("accountUserIds cannot be null or empty");
  }

  @Test
  public void shouldNotAcceptEmptyAccountUserIds() {
    // given
    PaymentsHistoryLambdaInput lambdaInput = PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(Collections.emptyList())
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("accountUserIds cannot be null or empty");
  }
}
