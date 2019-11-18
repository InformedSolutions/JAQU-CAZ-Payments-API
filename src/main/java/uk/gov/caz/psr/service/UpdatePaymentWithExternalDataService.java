package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A class which is responsible for getting an external status of a given payment, updating it in
 * the database and, if applicable, connecting it to an existing vehicle entrant entity.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class UpdatePaymentWithExternalDataService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final FinalizePaymentService finalizePaymentService;
  private final PaymentRepository internalPaymentsRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Gets an external status of {@code payment}, updates it in the database and, if applicable,
   * associates it with an existing vehicle entrant entity.
   *
   * @throws NullPointerException if {@code payment} is null
   * @throws NullPointerException if {@link Payment#getExternalId()} is null
   */
  public Payment updatePaymentWithExternalData(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(payment.getExternalId(), "External id cannot be null");

    String externalPaymentId = payment.getExternalId();
    Payment externalPayment =
        externalPaymentsRepository.findById(externalPaymentId)
            .map(getPaymentResult -> updateInternalPaymentWith(payment, getPaymentResult))
            .orElseThrow(() -> new IllegalStateException("External payment not found whereas the "
                + "internal one with id '" + payment.getId() + "' and external id " + "'"
                + externalPaymentId + "' exists"));

    return finalizePaymentProcessing(payment, externalPayment);
  }

  /**
   * Creates a lambda expression that updates the passed instance of {@link Payment} with the {@code
   * id}.
   */
  private Payment updateInternalPaymentWith(Payment payment, GetPaymentResult paymentInfo) {
    ExternalPaymentStatus newStatus = paymentInfo.getPaymentStatus();
    String emailAddress = paymentInfo.getEmail();
    return payment.toBuilder().externalPaymentStatus(newStatus)
        .authorisedTimestamp(getAuthorisedTimestamp(payment, newStatus))
        .vehicleEntrantPayments(payment.getVehicleEntrantPayments().stream()
            .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
                .internalPaymentStatus(InternalPaymentStatus.from(newStatus)).build())
            .collect(Collectors.toList()))
        .emailAddress(emailAddress).build();
  }

  /**
   * Updates payment's status in the database if changed and connects to an existing vehicle entrant
   * record if payment has been successfully processed.
   *
   * @param internalPayment An instance of {@link Payment} with its internal identifier set.
   * @param externalPayment An instance of {@link Payment} with its internal and external
   *        identifiers set.
   * @return An instance of {@link Payment} with vehicle entrants' ids set (if applicable).
   */
  private Payment finalizePaymentProcessing(Payment internalPayment, Payment externalPayment) {
    if (internalPayment.getExternalPaymentStatus() == externalPayment.getExternalPaymentStatus()) {
      log.info("External payment status is the same as the one from database and equal to '{}', "
          + "skipping updating the database", internalPayment.getExternalPaymentStatus());
      return externalPayment;
    }
    log.info(
        "Found the external payment, updating its status to '{}' in the database, "
            + "current status: '{}'",
        externalPayment.getExternalPaymentStatus(), internalPayment.getExternalPaymentStatus());
    Payment paymentWithVehicleEntrantsIds =
        finalizePaymentService.connectExistingVehicleEntrants(externalPayment);
    internalPaymentsRepository.update(paymentWithVehicleEntrantsIds);
    PaymentStatusUpdatedEvent event =
        new PaymentStatusUpdatedEvent(this, paymentWithVehicleEntrantsIds);
    applicationEventPublisher.publishEvent(event);
    return paymentWithVehicleEntrantsIds;
  }

  /**
   * Returns {@link LocalDateTime#now()} as the authorised payment timestamp (date and time when the
   * payment has been successfully processed by GOV UK Pay) provided {@code newExternalStatus} is
   * equal to {@link ExternalPaymentStatus#SUCCESS}. Otherwise, the current value is obtained from
   * {@code payment}.
   */
  private LocalDateTime getAuthorisedTimestamp(Payment payment,
      ExternalPaymentStatus newExternalStatus) {
    if (newExternalStatus == ExternalPaymentStatus.SUCCESS) {
      return LocalDateTime.now();
    }
    return payment.getAuthorisedTimestamp();
  }
}
