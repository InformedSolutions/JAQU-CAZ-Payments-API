package uk.gov.caz.psr.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.receipt.CustomPaymentReceiptEmailCreator;

/**
 * Service class responsible for creating {@link SendEmailRequest}s.
 */
@Service
@AllArgsConstructor
public class PaymentReceiptEmailCreator {

  private final List<CustomPaymentReceiptEmailCreator> emailReceiptRequestCreators;

  /**
   * Creates {@link SendEmailRequest} based on the passed {@code payment} by selecting the
   * appropriate {@link CustomPaymentReceiptEmailCreator}.
   *
   * @return An instance of {@link SendEmailRequest}.
   * @throws IllegalStateException if it is not possible to select a {@link
   *     CustomPaymentReceiptEmailCreator} to create the request.
   */
  public SendEmailRequest createSendEmailRequest(Payment payment) {
    return emailReceiptRequestCreators.stream()
        .filter(emailReceiptCreator -> emailReceiptCreator.isApplicableFor(payment))
        .findFirst()
        .map(emailReceiptCreator -> emailReceiptCreator.createSendEmailRequest(payment))
        .orElseThrow(() -> new IllegalStateException("Cannot determine the email receipt creator"));
  }
}
