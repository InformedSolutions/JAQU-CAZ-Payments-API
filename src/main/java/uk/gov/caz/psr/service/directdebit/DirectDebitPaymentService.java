package uk.gov.caz.psr.service.directdebit;

import com.gocardless.GoCardlessClient;
import com.gocardless.errors.InvalidApiUsageException;
import com.gocardless.errors.InvalidStateException;
import com.gocardless.resources.Payment;
import com.gocardless.services.PaymentService.PaymentCreateRequest.Currency;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.service.exception.CollectDirectDebitPaymentException;

/**
 * Service that obtains information about direct debit payments.
 */
@Service
@AllArgsConstructor
@Slf4j
public class DirectDebitPaymentService {

  private final AbstractGoCardlessClientFactory goCardlessClientFactory;

  /**
   * Obtains the registered direct debit mandates for the given account by its identifier {@code
   * accountId}. If the account does not exist, a list with only clean air zones is returned.
   */
  public DirectDebitPayment collectPayment(UUID cleanAirZoneId, int amount,
      Long reference, String mandateId) {
    try {
      log.info("Collect DirectDebitPayment: start");
      GoCardlessClient client = goCardlessClientFactory.createClientFor(cleanAirZoneId);

      Payment externalPayment = client.payments().create()
          .withAmount(amount)
          .withCurrency(Currency.GBP)
          .withReference(reference.toString())
          .withLinksMandate(mandateId)
          .execute();

      return DirectDebitPayment.from(externalPayment, mandateId);
    } catch (InvalidStateException | InvalidApiUsageException e) {
      log.error("Error while collecting the direct debit payment: '{}'", e.getMessage());
      throw new CollectDirectDebitPaymentException(e.getMessage());
    } finally {
      log.info("Collect DirectDebitPayment: finish");
    }
  }
}
