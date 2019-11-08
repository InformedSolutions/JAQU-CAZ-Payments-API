package uk.gov.caz.psr.dto.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.ExternalPaymentStatus;

@ExtendWith(MockitoExtension.class)
class GetPaymentResultTest {

  @Nested
  class GetStatus {

    @ParameterizedTest
    @ValueSource(strings = {"not_recognized", "invalid_status", "aaa"})
    public void shouldReturnUnknownStatusIfExternalStatusIsNotRecognized(String status) {
      // given
      GetPaymentResult result = createResultObject(status);

      // when
      ExternalPaymentStatus paymentStatus = result.getPaymentStatus();

      // then
      assertThat(paymentStatus).isEqualTo(ExternalPaymentStatus.UNKNOWN);
    }

    private GetPaymentResult createResultObject(String status) {
      return GetPaymentResult.builder()
          .state(PaymentState.builder().status(status).build())
          .build();
    }
  }
}