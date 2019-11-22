package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.domain.authentication.CredentialRetrievalManager;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
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

  private final CredentialRetrievalManager credentialRetrievalManager;
  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository internalPaymentsRepository;
  private final PaymentStatusUpdater paymentStatusUpdater;

  /**
   * Retrieves the payment by its internal identifier and, provided it exists and has an external
   * id, gets its external status and updates it in the database. Then the updated value is returned
   * to the caller. {@link Optional#empty()} is returned if the record is not found in the database
   * or it has {@code null} external identifier.
   *
   * @throws NullPointerException if {@code id} is null
   */
  public Optional<Payment> getExternalPaymentAndUpdateStatus(UUID id) {
    Preconditions.checkNotNull(id, "ID cannot be null");

    Payment payment = internalPaymentsRepository.findById(id).orElse(null);

    if (payment == null) {
      log.warn("Payment '{}' is absent in the database", id);
      return Optional.empty();
    }

    String externalPaymentId = payment.getExternalId();
    if (externalPaymentId == null) {
      log.warn("Payment '{} does not have an external id and its status will not be updated", id);
      return Optional.empty();
    }

    // Retrieve API key for appropriate Gov.UK Pay account
    // TODO: get caz id
    Optional<String> apiKey = credentialRetrievalManager.getApiKey(payment);
    if (apiKey.isPresent()) {
      externalPaymentsRepository.setApiKey(apiKey.get());
    } else {
      return Optional.empty();
    }

    GetPaymentResult paymentInfo = externalPaymentsRepository.findById(externalPaymentId)
        .orElseThrow(() -> new IllegalStateException(
            "External payment not found with id " + "'" + externalPaymentId + "'"));

    ExternalPaymentStatus externalStatus = paymentInfo.getPaymentStatus();

    if (payment.getExternalPaymentStatus() == externalStatus) {
      log.warn(
          "External payment status is the same as the one from database and equal to '{}', "
              + "the payment will not be updated in the database",
          payment.getExternalPaymentStatus());
      return Optional.of(payment);
    }
    Payment result = paymentStatusUpdater.updateWithStatus(payment, externalStatus,
        OnBeforePublishPaymentStatusUpdateEvent.buildPaymentWith(paymentInfo.getEmail()));
    return Optional.of(result);
  }
}
