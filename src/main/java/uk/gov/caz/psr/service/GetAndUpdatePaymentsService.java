package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.domain.authentication.CredentialRetrievalManager;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Class that handles getting external payments and updating its status in the
 * database.
 */
@Service
@AllArgsConstructor
@Slf4j
public class GetAndUpdatePaymentsService {

  private final CredentialRetrievalManager credentialRetrievalManager;
  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository internalPaymentsRepository;

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

    String externalPaymentId = internalPayment.getExternalPaymentId();
    if (externalPaymentId == null) {
      log.info(
          "Payment '{} does not have an external id and its status will not be updated",
          id);
      return Optional.empty();
    }

    // Retrieve API key for appropriate Gov.UK Pay account
    Optional<String> apiKey = credentialRetrievalManager
        .getApiKey(internalPayment.getCleanAirZoneName());
    if (apiKey.isPresent()) {
      externalPaymentsRepository.setApiKey(apiKey.get());
    } else {
      return Optional.empty();
    }

    Payment externalPayment =
        externalPaymentsRepository.findById(externalPaymentId)
            .map(updateExternalPaymentWithInternalId(internalPayment.getId()))
            .orElseThrow(() -> new IllegalStateException(
                "External payment not found whereas the "
                    + "internal one with id '" + id + "' and external id '"
                    + externalPaymentId + "' " + "exists"));

    log.info(
        "Found the external payment, updating its status to '{}' in the database",
        externalPayment.getStatus());
    internalPaymentsRepository.update(externalPayment);
    return Optional.of(externalPayment);
  }

  /**
   * Creates a lambda expression that updates the passed instance of
   * {@link Payment} with the {@code
   * id}.
   */
  private Function<Payment, Payment> updateExternalPaymentWithInternalId(
      UUID id) {
    return externalPayment -> externalPayment.toBuilder().id(id).build();
  }
}
