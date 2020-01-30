package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentStatusUpdate;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.exception.PaymentDoesNotExistException;
import uk.gov.caz.psr.service.exception.PaymentNotProcessedException;

/**
 * A service which updates {@code paymentStatus} for all found {@link EntrantPayment}.
 */
@Service
@Slf4j
@AllArgsConstructor
public class PaymentStatusUpdateService {

  private final EntrantPaymentRepository entrantPaymentRepository;
  private final PaymentRepository paymentRepository;

  /**
   * Process update of the {@link EntrantPayment} with provided details.
   *
   * @param entrantPaymentStatusUpdates list of {@link EntrantPaymentStatusUpdate} which
   *     contains data to find and update {@link EntrantPayment}.
   */
  @Transactional
  public void process(
      List<EntrantPaymentStatusUpdate> entrantPaymentStatusUpdates) {
    Preconditions.checkNotNull(entrantPaymentStatusUpdates,
        "entrantPaymentStatusUpdates cannot be null");

    for (EntrantPaymentStatusUpdate entrantPaymentStatusUpdate : entrantPaymentStatusUpdates) {
      Optional<EntrantPayment> entrantPayment = loadEntrantPayment(entrantPaymentStatusUpdate);

      if (entrantPayment.isPresent()) {
        handleUpdateEntrantPayment(entrantPayment.get(), entrantPaymentStatusUpdate);
      } else {
        log.error("Vehicle entry not found");
        throw new PaymentDoesNotExistException(entrantPaymentStatusUpdate.getVrn());
      }
    }
  }

  private void handleUpdateEntrantPayment(EntrantPayment entrantPayment,
      EntrantPaymentStatusUpdate entrantPaymentStatusUpdate) {
    verifyOptionalPayment(entrantPayment);

    entrantPaymentRepository
        .update(prepareUpdateEntrantPayment(entrantPayment, entrantPaymentStatusUpdate));

    log.info("Updated EntrantPayment from the following statusUpdate: {}",
        entrantPaymentStatusUpdate);
  }

  private void verifyOptionalPayment(EntrantPayment entrantPayment) {
    Payment payment = paymentRepository
        .findByEntrantPayment(entrantPayment.getCleanAirZoneEntrantPaymentId())
        .orElse(null);

    if (payment != null && payment.getExternalPaymentStatus()
        .isNotFinished()) {
      log.error("Payment with ID: {} is still being processed.", payment.getId());
      throw new PaymentNotProcessedException("Payment is still being processed");
    }
  }

  /**
   * Builds {@link EntrantPayment} with updated status.
   *
   * @param entrantPayment {@link EntrantPayment} object which need to be updated.
   * @param entrantPaymentStatusUpdate {@link EntrantPaymentStatusUpdate} which contains data to
   *     find and update {@link EntrantPayment}.
   */
  private EntrantPayment prepareUpdateEntrantPayment(EntrantPayment entrantPayment,
      EntrantPaymentStatusUpdate entrantPaymentStatusUpdate) {

    return entrantPayment.toBuilder()
        .internalPaymentStatus(entrantPaymentStatusUpdate.getPaymentStatus())
        .caseReference(entrantPaymentStatusUpdate.getCaseReference())
        .updateActor(EntrantPaymentUpdateActor.LA)
        .build();
  }

  /**
   * Loads {@link EntrantPayment} from the repository for the provided details.
   *
   * @param entrantPaymentStatusUpdate {@link EntrantPaymentStatusUpdate} which contains data to
   *     find and update {@link EntrantPayment}.
   */
  private Optional<EntrantPayment> loadEntrantPayment(
      EntrantPaymentStatusUpdate entrantPaymentStatusUpdate) {

    return entrantPaymentRepository.findOneByVrnAndCazEntryDate(
        entrantPaymentStatusUpdate.getCleanAirZoneId(),
        entrantPaymentStatusUpdate.getVrn(),
        entrantPaymentStatusUpdate.getDateOfCazEntry());
  }
}
