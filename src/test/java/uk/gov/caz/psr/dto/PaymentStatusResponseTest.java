package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
public class PaymentStatusResponseTest {

  private final String ANY_EXTERNAL_ID = "some-external-id";
  private final String ANY_CASE_REFERENCE = "some-case-reference";

  @Test
  public void shouldBuildCompletePaymentStatusFromPaymentStatus() {
    // given
    PaymentStatus paymentStatus = PaymentStatus.builder()
        .status(InternalPaymentStatus.PAID)
        .externalId(ANY_EXTERNAL_ID)
        .caseReference(ANY_CASE_REFERENCE)
        .build();

    // when
    PaymentStatusResponse result = PaymentStatusResponse.from(paymentStatus);

    // then
    assertThat(result.getPaymentProviderId()).isEqualTo(paymentStatus.getExternalId());
    assertThat(result.getCaseReference()).isEqualTo(paymentStatus.getCaseReference());
    assertThat(result.getPaymentStatus()).isEqualTo(ChargeSettlementPaymentStatus.PAID);
  }
}
