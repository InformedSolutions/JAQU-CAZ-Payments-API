package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.model.ExternalPaymentDetails;

@ExtendWith(MockitoExtension.class)
public class GetPaymentResultConverterTest {

  @InjectMocks
  private GetPaymentResultConverter getPaymentResultConverter;

  @Nested
  class ToExternalPaymentDetails {

    @Test
    public void shouldReturnsExternalPaymentDetailsClass() {
      // given
      GetPaymentResult getPaymentResult = createGetPaymentResult();

      // when
      ExternalPaymentDetails result = getPaymentResultConverter
          .toExternalPaymentDetails(getPaymentResult);

      // then
      assertThat(result).isInstanceOf(ExternalPaymentDetails.class);
    }

    private GetPaymentResult createGetPaymentResult() {
      return GetPaymentResult.builder()
          .email("test@email.com")
          .state(PaymentState.builder().status("SUCCESS").build())
          .build();
    }
  }
}
