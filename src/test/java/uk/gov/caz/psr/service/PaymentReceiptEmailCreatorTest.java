package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.receipt.CustomPaymentReceiptEmailCreator;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptEmailCreatorTest {

  private PaymentReceiptEmailCreator paymentReceiptEmailCreator;

  @Test
  public void shouldThrowIllegalStateExceptionWhenThereIsNoCreator() {
    // given
    Payment payment = Payments.forRandomDays();
    paymentReceiptEmailCreator = new PaymentReceiptEmailCreator(Collections.emptyList());

    // when
    Throwable throwable = catchThrowable(
        () -> paymentReceiptEmailCreator.createSendEmailRequest(payment));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot determine the email receipt creator");
  }

  @Test
  public void shouldCreateRequestByApplicableCreator() {
    // given
    Payment payment = Payments.forRandomDays();
    SendEmailRequest sendEmailRequest = createSendEmailRequest();
    List<CustomPaymentReceiptEmailCreator> creators = mockEmailCreators(payment, sendEmailRequest);
    paymentReceiptEmailCreator = new PaymentReceiptEmailCreator(creators);

    // when
    SendEmailRequest request = paymentReceiptEmailCreator.createSendEmailRequest(payment);

    // then
    assertThat(request).isEqualTo(sendEmailRequest);
  }

  private List<CustomPaymentReceiptEmailCreator> mockEmailCreators(Payment payment,
      SendEmailRequest sendEmailRequest) {
    CustomPaymentReceiptEmailCreator notApplicable = mock(CustomPaymentReceiptEmailCreator.class);
    CustomPaymentReceiptEmailCreator applicable = mock(CustomPaymentReceiptEmailCreator.class);
    when(notApplicable.isApplicableFor(payment)).thenReturn(false);
    when(applicable.isApplicableFor(payment)).thenReturn(true);
    when(applicable.createSendEmailRequest(payment)).thenReturn(sendEmailRequest);
    return Arrays.asList(notApplicable, applicable);
  }

  private SendEmailRequest createSendEmailRequest() {
    return SendEmailRequest.builder()
        .emailAddress("something@something.com")
        .personalisation("personalisation")
        .templateId("template")
        .build();
  }
}