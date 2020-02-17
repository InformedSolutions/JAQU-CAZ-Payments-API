package uk.gov.caz.psr.service.listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;
import uk.gov.caz.psr.service.CleanAirZoneNameGetterService;
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
  private final CleanAirZoneNameGetterService cleanAirZoneNameGetterService;

  /**
   * Processes a payment event (given that its external status is SUCCESS).
   *
   * @param event triggered on payment status check
   */
  @EventListener(condition = "#event.payment.externalPaymentStatus.name() == 'SUCCESS'")
  public void onPaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
    checkPreconditions(event);
    Payment payment = event.getPayment();
    UUID cleanAirZoneId = getCleanAirZoneId(payment);
    double totalAmount = currencyFormatter.parsePennies(payment.getTotalPaid());

    log.info("Processing email event for payment with ID: {}", payment.getId());
    
    try {
      String cazName = cleanAirZoneNameGetterService.fetch(cleanAirZoneId);
      String vrn = payment.getEntrantPayments().iterator().next().getVrn();
      List<String> datesPaidFor = formatTravelDates(payment);

      SendEmailRequest sendEmailRequest =
          paymentReceiptService.buildSendEmailRequest(payment.getEmailAddress(), totalAmount,
              cazName, payment.getReferenceNumber().toString(), vrn,
              payment.getExternalId(), datesPaidFor);
      messagingClient.publishMessage(sendEmailRequest);
    } catch (CleanAirZoneNotFoundException e) {
      log.error("Clean Air Zone not found in VCCS: {}", cleanAirZoneId);
    } catch (Exception e) {
      log.error("Payment receipt not sent to recipient with payment ID: {}", payment.getId(), e);
    }
  }

  /**
   * Returns a list of formatted travel dates for {@code payment}.
   */
  private List<String> formatTravelDates(Payment payment) {
    return payment.getEntrantPayments()
        .stream()
        .map(entrantPayment -> entrantPayment.getTravelDate()
            .format(DateTimeFormatter.ofPattern("dd MMMM YYYY")))
        .collect(Collectors.toList());
  }

  private void checkPreconditions(PaymentStatusUpdatedEvent event) {
    Payment payment = event.getPayment();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(payment.getEmailAddress()),
        "Email address cannot be null or empty");
  }

  /**
   * Retrieves the Clean Air Zone ID for the given {@link Payment}.
   *
   * @param payment an instance of a {@link Payment} object
   * @return a {@link UUID} representing a Clean Air Zone.
   */
  private UUID getCleanAirZoneId(Payment payment) {
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Vehicle entrant payments should not be empty");
    return payment.getEntrantPayments().iterator().next().getCleanAirZoneId();
  }
}