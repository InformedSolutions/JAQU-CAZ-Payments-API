package uk.gov.caz.psr.service.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.service.PaymentReceiptService;

@ExtendWith(MockitoExtension.class)
public class PaymentReceiptSenderTest {

  @InjectMocks
  PaymentReceiptSender paymentReceiptSender;

  @Mock
  MessagingClient messagingClient;

  @Mock
  PaymentReceiptService paymentReceiptService;

  PaymentStatusUpdatedEvent paymentStatusUpdatedEvent;

  String email = "test@test.com";
  int amount = 20;

  @BeforeEach
  void init() {
    Payment payment = Payment.builder().id(UUID.randomUUID())
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD).totalPaid(amount)
        .vehicleEntrantPayments(new ArrayList<VehicleEntrantPayment>()).emailAddress(email).build();
    paymentStatusUpdatedEvent = mock(PaymentStatusUpdatedEvent.class);
    Mockito.when(paymentStatusUpdatedEvent.getPayment()).thenReturn(payment);
  }

  @Test
  void handleCorrectPaymentObject() throws JsonProcessingException {

    SendEmailRequest sendEmailRequest = SendEmailRequest.builder().templateId("test-template-id")
        .emailAddress(email).personalisation("{\"amount\":20}").build();

    Mockito.when(paymentReceiptService.buildSendEmailRequest(email, amount))
        .thenReturn(sendEmailRequest);

    paymentReceiptSender.onPaymentStatusUpdated(paymentStatusUpdatedEvent);

    Mockito.verify(messagingClient, times(1)).publishMessage(sendEmailRequest);
  }

  @Test
  void cannotBuildAmountIntoPersonalisationJson() throws JsonProcessingException {
    Mockito.when(paymentReceiptService.buildSendEmailRequest(email, amount))
        .thenThrow(new JsonMappingException(null, "test exception"));

    paymentReceiptSender.onPaymentStatusUpdated(paymentStatusUpdatedEvent);

  }

  @Test
  void cannotWriteMessageBodyToString() throws JsonProcessingException {

    SendEmailRequest sendEmailRequest = SendEmailRequest.builder().templateId("test-template-id")
        .emailAddress(email).personalisation("{\"amount\":20}").build();

    Mockito.when(paymentReceiptService.buildSendEmailRequest(email, amount))
        .thenReturn(sendEmailRequest);

    Mockito.doThrow(new JsonParseException(null, "test exception")).when(messagingClient)
        .publishMessage(sendEmailRequest);

    paymentReceiptSender.onPaymentStatusUpdated(paymentStatusUpdatedEvent);

  }
}
