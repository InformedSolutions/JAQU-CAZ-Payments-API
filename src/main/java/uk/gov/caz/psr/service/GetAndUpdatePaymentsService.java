package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
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
  private final FinalizePaymentService finalizePaymentService;

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

    Payment internalPayment = internalPaymentsRepository.findById(id).orElse(null);

    if (internalPayment == null) {
      log.info("Payment '{}' is absent in the database", id);
      return Optional.empty();
    }

    String externalPaymentId = internalPayment.getExternalId();
    if (externalPaymentId == null) {
      log.info("Payment '{} does not have an external id and its status will not be updated", id);
      return Optional.empty();
    }

    Payment externalPayment = externalPaymentsRepository.findById(externalPaymentId)
        .map(getPaymentResult -> updateInternalPaymentWith(internalPayment, getPaymentResult))
        .orElseThrow(() -> new IllegalStateException("External payment not found whereas the "
            + "internal one with id '" + id + "' and external id '" + externalPaymentId + "' "
            + "exists"));

    return Optional.of(finalizePaymentProcessing(internalPayment, externalPayment));
  }

  /**
   * Updates payment's status in the database if changed and connects to an existing vehicle entrant
   * record if payment has been successfully processed.
   *
   * @param internalPayment An instance of {@link Payment} with its internal identifier set.
   * @param externalPayment An instance of {@link Payment} with its internal and external
   *     identifiers set.
   * @return An instance of {@link Payment} with vehicle entrants' ids set (if applicable).
   */
  private Payment finalizePaymentProcessing(Payment internalPayment, Payment externalPayment) {
    if (internalPayment.getExternalPaymentStatus() == externalPayment.getExternalPaymentStatus()) {
      log.info("External payment status is the same as the one from database and equal to '{}', "
          + "skipping updating the database", internalPayment.getExternalPaymentStatus());
      return externalPayment;
    }
    log.info("Found the external payment, updating its status to '{}' in the database, "
            + "current status: '{}'", externalPayment.getExternalPaymentStatus(),
        internalPayment.getExternalPaymentStatus());
    Payment paymentWithVehicleEntrantsIds = finalizePaymentService
        .connectExistingVehicleEntrants(externalPayment);
    internalPaymentsRepository.update(paymentWithVehicleEntrantsIds);
    return paymentWithVehicleEntrantsIds;
  }

  /**
   * Creates a lambda expression that updates the passed instance of {@link Payment} with the {@code
   * id}.
   */
  private Payment updateInternalPaymentWith(Payment payment, GetPaymentResult getPaymentResult) {
    ExternalPaymentStatus newExternalStatus = getPaymentResult.getPaymentStatus();
    LocalDateTime authorisedTimestamp = newExternalStatus == ExternalPaymentStatus.SUCCESS
        ? LocalDateTime.now() : payment.getAuthorisedTimestamp();
    return payment.toBuilder()
        .externalPaymentStatus(newExternalStatus)
        .authorisedTimestamp(authorisedTimestamp)
        .vehicleEntrantPayments(payment.getVehicleEntrantPayments()
            .stream()
            .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
                .internalPaymentStatus(InternalPaymentStatus.from(newExternalStatus))
                .build())
            .collect(Collectors.toList())
        )
        .build();
  }
}
