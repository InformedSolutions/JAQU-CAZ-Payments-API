package uk.gov.caz.psr.service.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.service.PaymentReceiptService;

/**
 * Listener to pick up email events and use messaging client to build and send an email.
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentReceiptSender {

  private final MessagingClient messagingClient;
  private final PaymentReceiptService paymentReceiptService;

  /**
   * Processes a payment event (given that its external status is SUCCESS).
   * 
   * @param event triggered on payment status check
   */
  @EventListener(condition = "#event.payment.externalPaymentStatus.name() == 'SUCCESS'")
  public void onPaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
    Payment payment = event.getPayment();

    log.info("Processing email event for payment with ID: {}", payment.getId());

    // build email object
    try {
      SendEmailRequest sendEmailRequest = paymentReceiptService
          .buildSendEmailRequest(payment.getEmailAddress(), payment.getTotalPaid());
      messagingClient.publishMessage(sendEmailRequest);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      log.error("Error occurred when attempting to serialize payment amount: {}",
          payment.getTotalPaid());
      log.error("Payment receipt not sent to recipient with payment ID: {}", payment.getId());
    }

  }

}
