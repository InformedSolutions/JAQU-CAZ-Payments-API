package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.psr.util.TestObjectFactory.Payments.preparePayment;
import static uk.gov.caz.psr.util.TestObjectFactory.Payments.preparePaymentModifications;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentDetailsResponse;
import uk.gov.caz.psr.service.AccountService;

@ExtendWith(MockitoExtension.class)
class PaymentDetailsConverterTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private AccountService accountService;

  @InjectMocks
  private PaymentDetailsConverter converter;

  @Test
  public void shouldConvertPaymentToPaymentDetailsResponse() {
    // given
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(40));
    given(accountService.getPayerName(any())).willReturn("Administrator");

    // when
    PaymentDetailsResponse paymentDetailsResponse = converter
        .toPaymentDetailsResponse(preparePayment(UUID.randomUUID()), preparePaymentModifications());

    // then
    assertThat(paymentDetailsResponse).isNotNull();
    assertThat(paymentDetailsResponse.getCentralPaymentReference()).isEqualTo(1500L);
    assertThat(paymentDetailsResponse.getPaymentProviderId()).isEqualTo("123");
    assertThat(paymentDetailsResponse.isTelephonePayment()).isFalse();
    assertThat(paymentDetailsResponse.getPayerName()).isEqualTo("Administrator");
    assertThat(paymentDetailsResponse.getTotalPaid()).isEqualTo(BigDecimal.valueOf(40));
    assertThat(paymentDetailsResponse.getLineItems()).hasSize(1);
    assertThat(paymentDetailsResponse.getLineItems().get(0).getChargePaid())
        .isEqualTo(BigDecimal.valueOf(40));
    assertThat(paymentDetailsResponse.getLineItems().get(0).getPaymentStatus())
        .isEqualTo(ChargeSettlementPaymentStatus.PAID);
    assertThat(paymentDetailsResponse.getLineItems().get(0).getVrn())
        .isEqualTo("CAS310");
  }
}