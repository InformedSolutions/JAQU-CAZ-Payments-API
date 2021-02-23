package uk.gov.caz.psr.service.listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;
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
  @Value("#{'${application.emails-to-skip}'.split(',')}")
  private final Set<String> emailsToSkip;
  private final PaymentRepository paymentRepository;

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
      boolean shouldBeSent = checkIfShouldBeSent(payment);
      if (shouldBeSent) {
        SendEmailRequest request = paymentReceiptEmailCreator.createSendEmailRequest(payment);
        messagingClient.publishMessage(request);
        setEmailConfirmationSentForPayment(payment);
        log.info("Email for payment with ID {} has been sent", payment.getId());
      } else {
        log.info("Skipping email sending for payment with ID: {}", payment.getId());
      }
    } catch (Exception e) {
      log.error("Payment receipt not sent to recipient with payment ID: {}", payment.getId(), e);
    } finally {
      log.info("Finished processing email event for payment with ID: {}", payment.getId());
    }
  }

  private void setEmailConfirmationSentForPayment(Payment payment) {
    paymentRepository.markSentConfirmationEmail(payment.getId());
  }

  private boolean checkIfShouldBeSent(Payment payment) {
    return !emailsToSkip.contains(payment.getEmailAddress());
  }

  private void checkPreconditions(Payment payment) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(payment.getEmailAddress()),
        "Email address cannot be null or empty");
  }
}