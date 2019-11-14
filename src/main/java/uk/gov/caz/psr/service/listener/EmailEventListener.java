package uk.gov.caz.psr.service.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.SendEmailEvent;
import uk.gov.caz.psr.service.PaymentReceiptService;

/**
 * Listener to pick up email events and use messaging client to build and send an email.
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailEventListener implements ApplicationListener<SendEmailEvent> {

  private final MessagingClient messagingClient;
  private final PaymentReceiptService paymentReceiptService;

  @Override
  public void onApplicationEvent(SendEmailEvent event) {
    Payment payment = event.getPayment();

    if (payment.getExternalPaymentStatus().equals(ExternalPaymentStatus.SUCCESS)) {

      log.info("Processing email event for payment with ID: {}", payment.getId());

      // build email object
      try {
        SendEmailRequest sendEmailRequest = paymentReceiptService
            .buildSendEmailRequest(payment.getEmailAddress(), payment.getTotalPaid());
        messagingClient.publishMessage(sendEmailRequest);
      } catch (JsonProcessingException e) {
        log.error(e.getMessage());
        log.error("Error occurred when attempting to serialize payment amount: {}",
            payment.getTotalPaid().toString());
      }
    }

  }

}
