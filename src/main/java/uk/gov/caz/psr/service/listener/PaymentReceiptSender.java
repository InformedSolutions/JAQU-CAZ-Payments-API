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
import uk.gov.caz.psr.service.PaymentReceiptService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Listener to pick up email events and use messaging client to build and send an email.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentReceiptSender {

  private final CurrencyFormatter currencyFormatter;
  private final MessagingClient messagingClient;
  private final PaymentReceiptService paymentReceiptService;

  /**
   * Processes a payment event (given that its external status is SUCCESS).
   *
   * @param event triggered on payment status check
   */
  @EventListener(condition = "#event.payment.externalPaymentStatus.name() == 'SUCCESS'")
  public void onPaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
    checkPreconditions(event);
    Payment payment = event.getPayment();
    double totalAmount = currencyFormatter.parsePennies(payment.getTotalPaid());

    log.info("Processing email event for payment with ID: {}", payment.getId());
    
    String cazName = payment.getCleanAirZoneName();
    String vrn = payment.getEntrantPayments().iterator().next().getVrn();
    
    try {
      SendEmailRequest sendEmailRequest =
          paymentReceiptService.buildSendEmailRequest(payment.getEmailAddress(), totalAmount, 
              cazName, payment.getReferenceNumber().toString(), vrn, payment.getExternalId());
      messagingClient.publishMessage(sendEmailRequest);
    } catch (Exception e) {
      log.error("Payment receipt not sent to recipient with payment ID: {}", payment.getId(), e);
    }
  }

  private void checkPreconditions(PaymentStatusUpdatedEvent event) {
    Payment payment = event.getPayment();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(payment.getEmailAddress()),
        "Email address cannot be null or empty");
  }
}
