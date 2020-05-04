package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;

@ExtendWith(MockitoExtension.class)
class InitiatePaymentRequestToModelConverterTest {

  @Nested
  class WithoutUserId {
    @Test
    public void shouldNotContainUserId() {
      // given
      String userId = null;
      UUID cleanAirZoneId = UUID.fromString("196dc608-25dc-42aa-ad99-9a834697ef87");
      InitiatePaymentRequest request = InitiatePaymentRequest.builder()
          .telephonePayment(Boolean.TRUE)
          .userId(userId)
          .returnUrl("http://localhost")
          .cleanAirZoneId(cleanAirZoneId)
          .transactions(Collections.emptyList())
          .build();

      // when
      Payment payment = InitiatePaymentRequestToModelConverter.toPayment(request);

      // then
      assertThat(payment.getExternalPaymentStatus()).isEqualTo(ExternalPaymentStatus.INITIATED);
      assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_DEBIT_CARD);
      assertThat(payment.getEntrantPayments()).isEmpty();
      assertThat(payment.getCleanAirZoneId()).isEqualTo(cleanAirZoneId);
      assertThat(payment.getUserId()).isNull();
      assertThat(payment.isTelephonePayment()).isTrue();
    }
  }

  @Nested
  class WithUserId {

    @Test
    public void shouldContainUserId() {
      // given
      String userId = "06ab9c88-24ac-4033-87f7-289ac1ce13fd";
      UUID cleanAirZoneId = UUID.fromString("196dc608-25dc-42aa-ad99-9a834697ef87");
      InitiatePaymentRequest request = InitiatePaymentRequest.builder()
          .telephonePayment(Boolean.FALSE)
          .userId(userId)
          .returnUrl("http://localhost")
          .cleanAirZoneId(cleanAirZoneId)
          .transactions(Collections.emptyList())
          .build();

      // when
      Payment payment = InitiatePaymentRequestToModelConverter.toPayment(request);

      // then
      assertThat(payment.getExternalPaymentStatus()).isEqualTo(ExternalPaymentStatus.INITIATED);
      assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_DEBIT_CARD);
      assertThat(payment.getEntrantPayments()).isEmpty();
      assertThat(payment.getCleanAirZoneId()).isEqualTo(cleanAirZoneId);
      assertThat(payment.getUserId()).isEqualTo(UUID.fromString(userId));
      assertThat(payment.isTelephonePayment()).isFalse();
    }
  }
}