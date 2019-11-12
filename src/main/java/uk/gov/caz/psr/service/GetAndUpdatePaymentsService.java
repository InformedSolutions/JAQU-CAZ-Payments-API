package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Class that handles getting external payments and updating its status in the
 * database.
 */
@Service
@AllArgsConstructor
@Slf4j
public class GetAndUpdatePaymentsService {

  private final PaymentRepository internalPaymentsRepository;
  private final UpdatePaymentWithExternalDataService updatePaymentWithExternalDataService;

  /**
   * Retrieves the payment by its internal identifier and, provided it exists
   * and has an external id, gets its external status and updates it in the
   * database. Then the updated value is returned to the caller.
   * {@link Optional#empty()} is returned if the record is not found in the
   * database or it has {@code null} external identifier.
   *
   * @throws NullPointerException if {@code id} is null
   */
  public Optional<Payment> getExternalPaymentAndUpdateStatus(UUID id) {
    Preconditions.checkNotNull(id, "ID cannot be null");

    Payment internalPayment =
        internalPaymentsRepository.findById(id).orElse(null);

    if (internalPayment == null) {
      log.info("Payment '{}' is absent in the database", id);
      return Optional.empty();
    }

    String externalPaymentId = internalPayment.getExternalId();
    if (externalPaymentId == null) {
      log.info(
          "Payment '{} does not have an external id and its status will not be updated",
          id);
      return Optional.empty();
    }

    return Optional.of(updatePaymentWithExternalDataService
        .updatePaymentWithExternalData(internalPayment));
  }
}
