package uk.gov.caz.psr.dto.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPaymentResultTest {

  @Test
  public void testConvertingStatusToEnum() {
    // given
    GetPaymentResult input = GetPaymentResult.builder()
        .state(PaymentState.builder().status("unrecognised").build())
        .build();

    // when
    Throwable throwable = catchThrowable(() -> input.getPaymentStatus());

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }
}