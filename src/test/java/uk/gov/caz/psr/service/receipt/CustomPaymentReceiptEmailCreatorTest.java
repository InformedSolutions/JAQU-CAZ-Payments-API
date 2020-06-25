package uk.gov.caz.psr.service.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
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
class CustomPaymentReceiptEmailCreatorTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private CleanAirZoneService cleanAirZoneNameGetterService;

  @Mock
  private ObjectMapper objectMapper;

  private CustomPaymentReceiptEmailCreator paymentReceiptEmailCreator;

  @BeforeEach
  public void setUpReceiptRequestCreator() {
    paymentReceiptEmailCreator = new SamplePaymentReceiptEmailCreator(
        currencyFormatter, cleanAirZoneNameGetterService, objectMapper, "some-template"
    );
  }

  @Test
  public void shouldRethrowJsonProcessingException() throws JsonProcessingException {
    // given
    when(objectMapper.writeValueAsString(any())).thenThrow(new MockedJsonProcessingException(""));
    Payment payment = randomPayment();

    // when
    Throwable throwable = catchThrowable(
        () -> paymentReceiptEmailCreator.createSendEmailRequest(payment));

    // then
    assertThat(throwable).isInstanceOf(MockedJsonProcessingException.class);
  }

  @Nested
  class GetCazName {

    @Test
    public void shouldThrowIllegalArgumentExceptionIfEntrantPaymentsAreEmpty() {
      // given
      Payment payment = paymentWithoutEntrantPayments();

      // when
      Throwable throwable = catchThrowable(
          () -> paymentReceiptEmailCreator.getCazName(payment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    private Payment paymentWithoutEntrantPayments() {
      return randomPayment().toBuilder().entrantPayments(Collections.emptyList()).build();
    }
  }

  private Payment randomPayment() {
    return Payments.forRandomDays();
  }

  private static class MockedJsonProcessingException extends JsonProcessingException {

    protected MockedJsonProcessingException(String msg) {
      super(msg);
    }
  }

  private static class SamplePaymentReceiptEmailCreator extends CustomPaymentReceiptEmailCreator {

    public SamplePaymentReceiptEmailCreator(CurrencyFormatter currencyFormatter,
        CleanAirZoneService cleanAirZoneNameGetterService,
        ObjectMapper objectMapper, String templateId) {
      super(currencyFormatter, cleanAirZoneNameGetterService, objectMapper, templateId);
    }

    @Override
    public boolean isApplicableFor(Payment payment) {
      return true;
    }

    @Override
    Map<String, Object> createPersonalisationPayload(Payment payment) {
      return Collections.emptyMap();
    }
  }
}