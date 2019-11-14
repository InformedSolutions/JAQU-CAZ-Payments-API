package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository;

/**
 * A service responsible for 'cleaning up' a single dangling payment.
 */
@Service
@Slf4j
@AllArgsConstructor
public class CleanupDanglingPaymentService {

  private final VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;
  private final PaymentStatusUpdater paymentStatusUpdater;
  private final ExternalPaymentsRepository externalPaymentsRepository;

  /**
   * Processes the passed {@code danglingPayment}: gets the external status and updates it in the
   * database.
   */
  public void processDanglingPayment(Payment danglingPayment) {
    checkPreconditions(danglingPayment);

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      String externalId = danglingPayment.getExternalId();
      GetPaymentResult paymentInfo = externalPaymentsRepository.findById(externalId)
          .orElseThrow(() -> new IllegalStateException("External payment not found with id "
              + "'" + externalId + "'"));
      ExternalPaymentStatus status = paymentInfo.getPaymentStatus();
      if (status == danglingPayment.getExternalPaymentStatus()) {
        log.info("The status of a dangling payment has not changed and is equal to '{}', "
            + "aborting the update", danglingPayment.getExternalPaymentStatus());
        return;
      }

      log.info("Updating status of payment '{}' from '{}' to '{}'", danglingPayment.getId(),
          danglingPayment.getExternalPaymentStatus(), status);

      paymentStatusUpdater.updateWithStatus(loadVehicleEntrantPayments(danglingPayment),
          status, OnBeforePublishPaymentStatusUpdateEvent.buildPaymentWith(paymentInfo.getEmail()));
    } finally {
      long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      log.info("Processing the dangling payment '{}' took {}ms", danglingPayment.getId(), elapsed);
    }
  }

  /**
   * Verifies whether passed {@code danglingPayment} is in valid state when calling {@link
   * CleanupDanglingPaymentService#processDanglingPayment(uk.gov.caz.psr.model.Payment)}.
   */
  private void checkPreconditions(Payment danglingPayment) {
    Preconditions.checkNotNull(danglingPayment, "Payment cannot be null");
    Preconditions.checkNotNull(danglingPayment.getExternalId(), "External id cannot be null");
    Preconditions.checkArgument(danglingPayment.getVehicleEntrantPayments().isEmpty(),
        "Vehicle entrant payments should be empty");
  }

  /**
   * For the passed {@code payment} it loads all associated vehicle entrant records. A new {@link
   * Payment} instance is created with the newly fetched records ann all previous attributes from
   * {@code payment}.
   */
  private Payment loadVehicleEntrantPayments(Payment payment) {
    return payment.toBuilder()
        .vehicleEntrantPayments(vehicleEntrantPaymentRepository.findByPaymentId(payment.getId()))
        .build();
  }
}
