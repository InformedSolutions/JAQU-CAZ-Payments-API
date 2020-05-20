package uk.gov.caz.psr.service.listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.service.PaymentReceiptEmailCreator;

/**
 * Listener to pick up email events and use messaging client to build and send an email.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentReceiptSender {

  private final MessagingClient messagingClient;
  private final PaymentReceiptEmailCreator paymentReceiptEmailCreator;

  /**
   * Processes a payment event (given that its external status is SUCCESS).
   *
   * @param event triggered on payment status check
   */
  @EventListener(condition = "#event.payment.externalPaymentStatus.name() == 'SUCCESS'")
  public void onPaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
    Payment payment = event.getPayment();
    try {
      checkPreconditions(payment);
      log.info("Processing email event for payment with ID: {}", payment.getId());
      SendEmailRequest request = paymentReceiptEmailCreator.createSendEmailRequest(payment);
      messagingClient.publishMessage(request);
    } catch (Exception e) {
      log.error("Payment receipt not sent to recipient with payment ID: {}", payment.getId(), e);
    } finally {
      log.info("Finished processing email event for payment with ID: {}", payment.getId());
    }
  }

  private void checkPreconditions(Payment payment) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(payment.getEmailAddress()),
        "Email address cannot be null or empty");
  }
}