package uk.gov.caz.psr.service.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.service.PaymentReceiptService;
import uk.gov.caz.psr.util.CurrencyFormatter;

@ExtendWith(MockitoExtension.class)
public class PaymentReceiptSenderTest {

  private static final String ANY_VALID_EMAIL = "test@test.com";
  private static final int ANY_AMOUNT = 800;
  private static final Payment ANY_PAYMENT = Payment.builder()
      .id(UUID.randomUUID())
      .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
      .totalPaid(ANY_AMOUNT)
      .vehicleEntrantPayments(Collections.emptyList())
      .emailAddress(ANY_VALID_EMAIL).build();

  @Mock
  CurrencyFormatter currencyFormatter;

  @Mock
  MessagingClient messagingClient;

  @Mock
  PaymentReceiptService paymentReceiptService;

  @InjectMocks
  PaymentReceiptSender paymentReceiptSender;

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenEmailIsNull() {
    // given
    PaymentStatusUpdatedEvent event = eventWithNullEmail();

    // when
    Throwable throwable = catchThrowable(() -> paymentReceiptSender.onPaymentStatusUpdated(event));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email address cannot be null or empty");
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenEmailIsEmpty() {
    // given
    PaymentStatusUpdatedEvent event = eventWithEmptyEmail();

    // when
    Throwable throwable = catchThrowable(() -> paymentReceiptSender.onPaymentStatusUpdated(event));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email address cannot be null or empty");
  }

  @Test
  void shouldHandleCorrectPaymentObject() throws JsonProcessingException {
    // given
    SendEmailRequest sendEmailRequest = anyValidRequest();
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, ANY_PAYMENT);
    when(currencyFormatter.parsePennies(ANY_AMOUNT)).thenReturn(8.0);
    when(paymentReceiptService.buildSendEmailRequest(ANY_VALID_EMAIL, 8.0))
        .thenReturn(sendEmailRequest);

    // when
    paymentReceiptSender.onPaymentStatusUpdated(event);

    // then
    verify(messagingClient, times(1)).publishMessage(sendEmailRequest);
  }

  @Test
  void shouldNotPropagateExceptionUponSerializationError() throws JsonProcessingException {
    // given
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, ANY_PAYMENT);
    double amount = 8.0;
    when(currencyFormatter.parsePennies(ANY_AMOUNT)).thenReturn(amount);
    when(paymentReceiptService.buildSendEmailRequest(ANY_VALID_EMAIL, amount))
        .thenThrow(new JsonMappingException(null, "test exception"));
    when(currencyFormatter.parsePennies(ANY_AMOUNT)).thenReturn((double) ANY_AMOUNT);

    // when
    paymentReceiptSender.onPaymentStatusUpdated(event);

    // then
    verify(messagingClient, never()).publishMessage(any());
  }

  @Test
  void shouldNotPropagateExceptionUponMessagePublicationError() throws JsonProcessingException {
    // given
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, ANY_PAYMENT);
    SendEmailRequest sendEmailRequest = anyValidRequest();
    when(currencyFormatter.parsePennies(ANY_AMOUNT)).thenReturn(8.0);
    when(paymentReceiptService.buildSendEmailRequest(ANY_VALID_EMAIL, 8.0))
        .thenReturn(sendEmailRequest);

    // when
    Throwable throwable = catchThrowable(() -> paymentReceiptSender.onPaymentStatusUpdated(event));

    // then
    assertThat(throwable).isNull();
  }

  private SendEmailRequest anyValidRequest() {
    return SendEmailRequest.builder().templateId("test-template-id").emailAddress(ANY_VALID_EMAIL)
        .personalisation("{\"amount\":" + ANY_AMOUNT + "}").build();
  }

  private PaymentStatusUpdatedEvent eventWithNullEmail() {
    return new PaymentStatusUpdatedEvent(this, buildPaymentWithEmail(null));
  }

  private PaymentStatusUpdatedEvent eventWithEmptyEmail() {
    return new PaymentStatusUpdatedEvent(this, buildPaymentWithEmail(""));
  }

  private Payment buildPaymentWithEmail(String s) {
    return ANY_PAYMENT.toBuilder().emailAddress(s).build();
  }
}
