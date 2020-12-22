package uk.gov.caz.psr.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.exception.PaymentNotFoundException;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentReceiptService {

  private final MessagingClient messagingClient;
  private final PaymentReceiptEmailCreator paymentReceiptEmailCreator;
  private final PaymentRepository paymentRepository;

  /**
   * Method selects proper payment receipt template using {@link PaymentReceiptEmailCreator} and
   * sends it to the provided email address.
   *
   * @param referenceNumber identifies the payment
   * @param email email address of the receipt receiver
   */
  public void sendReceipt(Long referenceNumber, String email) {
    Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
        .orElseThrow(() -> new PaymentNotFoundException(
            "Payment with provided reference number does not exist"));

    Payment paymentWithEmail = payment.toBuilder().emailAddress(email).build();
    SendEmailRequest request = paymentReceiptEmailCreator
        .createSendEmailRequest(paymentWithEmail);
    messagingClient.publishMessage(request);
  }
}
