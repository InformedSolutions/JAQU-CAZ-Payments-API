package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

public class PaymentStatusResponseTest {

  private final String ANY_EXTERNAL_ID = "some-external-id";
  private final String ANY_CASE_REFERENCE = "some-case-reference";

  @Test
  public void shouldBuildCompletePaymentStatusFromPaymentStatus() {
    // given
    PaymentStatus paymentStatus = PaymentStatus.builder()
        .paymentMethod(PaymentMethod.DIRECT_DEBIT)
        .status(InternalPaymentStatus.PAID)
        .externalId(ANY_EXTERNAL_ID)
        .caseReference(ANY_CASE_REFERENCE)
        .paymentProviderMandateId(ANY_EXTERNAL_ID)
        .build();

    // when
    PaymentStatusResponse result = PaymentStatusResponse.from(paymentStatus);

    // then
    assertThat(result.getPaymentProviderId()).isEqualTo(paymentStatus.getExternalId());
    assertThat(result.getCaseReference()).isEqualTo(paymentStatus.getCaseReference());
    assertThat(result.getPaymentStatus()).isEqualTo(ChargeSettlementPaymentStatus.PAID);
    assertThat(result.getPaymentMethod()).isEqualTo(ChargeSettlementPaymentMethod.DIRECT_DEBIT);
    assertThat(result.getPaymentMandateId()).isEqualTo(ANY_EXTERNAL_ID);
  }

  @Test
  public void shouldNotIncludePaymentMandateIDIfPaymentMethodIsNotDirectDebit() {
    // given
    PaymentStatus paymentStatus = PaymentStatus.builder()
        .status(InternalPaymentStatus.PAID)
        .paymentMethod(PaymentMethod.DIRECT_DEBIT)
        .paymentProviderMandateId(UUID.randomUUID().toString())
        .build();

    // when
    PaymentStatusResponse result = PaymentStatusResponse.from(paymentStatus);

    // then
    assertThat(result.getPaymentProviderId()).isNull();
  }
}
