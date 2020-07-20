package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.psr.util.TestObjectFactory.Payments.preparePayment;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentDetailsResponse;

@ExtendWith(MockitoExtension.class)
class PaymentDetailsConverterTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @InjectMocks
  private PaymentDetailsConverter converter;

  @Test
  public void shouldConvertPaymentToPaymentDetailsResponse() {
    // given
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(40));

    // when
    PaymentDetailsResponse paymentDetailsResponse = converter
        .toPaymentDetailsResponse(preparePayment(UUID.randomUUID()));

    // then
    assertThat(paymentDetailsResponse).isNotNull();
    assertThat(paymentDetailsResponse.getCentralPaymentReference()).isEqualTo(1500L);
    assertThat(paymentDetailsResponse.getPaymentProviderId()).isEqualTo("123");
    assertThat(paymentDetailsResponse.isTelephonePayment()).isFalse();
    assertThat(paymentDetailsResponse.getTotalPaid()).isEqualTo(BigDecimal.valueOf(40));
    assertThat(paymentDetailsResponse.getLineItems()).hasSize(1);
    assertThat(paymentDetailsResponse.getLineItems().get(0).getChargePaid())
        .isEqualTo(BigDecimal.valueOf(40));
    assertThat(paymentDetailsResponse.getLineItems().get(0).getPaymentStatus())
        .isEqualTo(ChargeSettlementPaymentStatus.PAID);
  }
}