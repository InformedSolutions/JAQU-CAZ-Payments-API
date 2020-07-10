package uk.gov.caz.psr.service.receipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class OfflinePaymentReceiptEmailCreatorTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private CleanAirZoneService cleanAirZoneNameGetterService;

  @Mock
  private ObjectMapper objectMapper;

  private OfflinePaymentReceiptEmailCreator offlinePaymentReceiptEmailCreator;

  @BeforeEach
  public void setUpReceiptRequestCreator() {
    String templateId = "some-template";
    offlinePaymentReceiptEmailCreator = new OfflinePaymentReceiptEmailCreator(
        currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId
    );
  }

  @Nested
  class Applicability {

    @Test
    public void shouldReturnTrueForOfflinePayment() {
      // given
      Payment payment = offlinePayment();

      // when
      boolean result = offlinePaymentReceiptEmailCreator.isApplicableFor(payment);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForOnlinePayment() {
      // given
      Payment payment = onlinePayment();

      // when
      boolean result = offlinePaymentReceiptEmailCreator.isApplicableFor(payment);

      // then
      assertThat(result).isFalse();
    }

    private Payment paymentWithNullUserId() {
      return Payments.forRandomDays().toBuilder().userId(null).build();
    }

    private Payment offlinePayment() {
      return Payments.forRandomDays().toBuilder().telephonePayment(true).build();
    }

    private Payment onlinePayment() {
      return Payments.forRandomDays().toBuilder().telephonePayment(false).build();
    }
  }

}