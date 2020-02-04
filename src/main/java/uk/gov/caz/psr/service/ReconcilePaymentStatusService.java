package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.GetPaymentResultConverter;

/**
 * Class that handles getting external payments and updating its status in the database.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ReconcilePaymentStatusService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository internalPaymentsRepository;
  private final PaymentStatusUpdater paymentStatusUpdater;
  private final GetPaymentResultConverter getPaymentResultConverter;

  /**
   * Retrieves the payment by its internal identifier and, provided it exists and has an external
   * id, gets its external status and updates it in the database. Then the updated value is returned
   * to the caller. {@link Optional#empty()} is returned if the record is not found in the database
   * or it has {@code null} external identifier.
   *
   * @throws NullPointerException if {@code id} is null
   */
  public Optional<Payment> reconcilePaymentStatus(UUID id) {
    Preconditions.checkNotNull(id, "ID cannot be null");
    Payment payment = internalPaymentsRepository.findById(id).orElse(null);

    if (payment == null) {
      log.warn("Payment '{}' not found in the database", id);

      return Optional.empty();
    }

    String externalPaymentId = payment.getExternalId();
    if (externalPaymentId == null) {
      log.warn("Payment '{} does not have an external id and its status will not be updated", id);
      return Optional.empty();
    }

    UUID cleanAirZoneId = getCleanAirZoneId(payment);
    GetPaymentResult paymentInfo =
        externalPaymentsRepository.findByIdAndCazId(externalPaymentId, cleanAirZoneId)
            .orElseThrow(() -> new IllegalStateException(
                "External payment not found with id " + "'" + externalPaymentId + "'"));
    ExternalPaymentDetails externalPaymentDetails =
        getPaymentResultConverter.toExternalPaymentDetails(paymentInfo);

    if (hasSameStatus(payment, externalPaymentDetails)) {
      log.warn(
          "External payment status is the same as the one from database and equal to '{}', "
              + "the payment will not be updated in the database",
          payment.getExternalPaymentStatus());
      return Optional.of(payment);
    }

    Payment paymentWithEmail = payment.toBuilder().emailAddress(paymentInfo.getEmail()).build();

    Payment result = paymentStatusUpdater.updateWithExternalPaymentDetails(paymentWithEmail,
        externalPaymentDetails);
    return Optional.of(result);
  }

  /**
   * Method which check if payment has the same status as payment in GOV.UK PAY
   * 
   * @param payment payment object from DB
   * @param externalPaymentDetails data about payment from GOV.UK PAY
   * @return boolean value if payment has the same status as externalPaymentDetails
   */
  private boolean hasSameStatus(Payment payment, ExternalPaymentDetails externalPaymentDetails) {
    return payment.getExternalPaymentStatus() == externalPaymentDetails.getExternalPaymentStatus();
  }
  
  /**
   * Retrieves the Clean Air Zone ID for the given {@link Payment}.
   * @param payment an instance of a {@link Payment} object
   * @return a {@link UUID} representing a Clean Air Zone.
   */
  private UUID getCleanAirZoneId(Payment payment) {
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "CAZ entrant payments should not be empty");
    return payment.getEntrantPayments().iterator().next().getCleanAirZoneId();
  }
}
