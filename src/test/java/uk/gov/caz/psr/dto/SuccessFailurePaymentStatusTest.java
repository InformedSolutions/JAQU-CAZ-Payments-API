package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.ExternalPaymentStatus;

@ExtendWith(MockitoExtension.class)
class SuccessFailurePaymentStatusTest {

  @Nested
  class FactoryMethod {

    @Test
    public void shouldReturnSuccessIfExternalStatusIsSuccess() {
      // given
      ExternalPaymentStatus status = ExternalPaymentStatus.SUCCESS;

      // when
      SuccessFailurePaymentStatus result = SuccessFailurePaymentStatus.from(status);

      // then
      assertThat(result).isEqualTo(SuccessFailurePaymentStatus.SUCCESS);
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.psr.dto.SuccessFailurePaymentStatusTest#notSuccessStatuses")
    public void shouldReturnFailureIfExternalStatusIsNotSuccess(ExternalPaymentStatus status) {
      // when
      SuccessFailurePaymentStatus result = SuccessFailurePaymentStatus.from(status);

      // then
      assertThat(result).isEqualTo(SuccessFailurePaymentStatus.FAILURE);
    }
  }

  static ExternalPaymentStatus[] notSuccessStatuses() {
    return EnumSet.complementOf(
        EnumSet.of(ExternalPaymentStatus.SUCCESS)
    ).toArray(new ExternalPaymentStatus[0]);
  }
}