package uk.gov.caz.psr.service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Class that handles getting external payments and updating its status in the database.
 */
@Service
@AllArgsConstructor
@Slf4j
public class GetAndUpdatePaymentsService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository internalPaymentsRepository;

  /**
   * Retrieves the payment by its internal identifier and, provided it exists and has an external
   * id, gets its external status and updates it in the database. Then the updated value is returned
   * to the caller. {@link Optional#empty()} is returned if the record is not found in the database
   * or it has {@code null} external identifier.
   */
  public Optional<Payment> getExternalPaymentAndUpdateStatus(UUID id) {
    Payment internalPayment = internalPaymentsRepository.findById(id).orElse(null);

    if (internalPayment == null) {
      log.info("Payment '{}' is absent in the database", id);
      return Optional.empty();
    }

    Optional<Payment> externalPayment = Optional.ofNullable(internalPayment.getExternalPaymentId())
        .flatMap(externalPaymentsRepository::findById)
        .map(updateExternalPaymentWithInternalId(internalPayment.getId()));

    externalPayment.ifPresent(payment -> {
      log.info("Found the external payment, updating its status to '{}' in the database",
          payment.getStatus());
      internalPaymentsRepository.update(payment);
    });

    return externalPayment;
  }

  /**
   * Creates a lambda expression that updates the passed instance of {@link Payment} with the {@code
   * id}.
   */
  private Function<Payment, Payment> updateExternalPaymentWithInternalId(UUID id) {
    return externalPayment -> externalPayment.toBuilder().id(id).build();
  }
}
