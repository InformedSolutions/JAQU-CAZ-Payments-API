package uk.gov.caz.psr.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentDetailsResponse;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.service.exception.PaymentNotFoundException;
import uk.gov.caz.psr.util.PaymentDetailsConverter;
import uk.gov.caz.psr.util.ReferencesHistoryConverter;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentDetailRepository paymentDetailRepository;
  private final ReferencesHistoryConverter referencesHistoryConverter;
  private final PaymentDetailsConverter paymentDetailsConverter;

  /**
   * Finds a given payment by its internal identifier passed as {@code paymentId}.
   *
   * @param paymentId An internal identifier of the payment.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   * @throws PaymentNotFoundException when {@link Payment} with the provided referenceNumber is
   *     not stored in the database.
   */
  public PaymentDetailsResponse getPayment(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException(
            "Payment with provided paymentId does not exist"));

    return paymentDetailsConverter.toPaymentDetailsResponse(payment,
        getPaymentModificationsListForPayment(payment));
  }

  /**
   * Finds a given payment by central reference number passed as {@code referenceNumber}.
   *
   * @param referenceNumber central reference number.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException if {@code referenceNumber} is null
   * @throws PaymentNotFoundException when {@link Payment} with the provided referenceNumber is
   *     not stored in the database.
   */
  public ReferencesHistoryResponse getPaymentHistoryByReferenceNumber(Long referenceNumber) {
    Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
        .orElseThrow(() -> new PaymentNotFoundException(
            "Payment with provided reference number does not exist"));

    return referencesHistoryConverter.toReferencesHistoryResponse(payment,
        getPaymentModificationsListForPayment(payment));
  }

  /**
   * Finds a list of {@link PaymentModification} for the provided {@link Payment}.
   *
   * @return List of {@link PaymentModification}
   */
  private List<PaymentModification> getPaymentModificationsListForPayment(Payment payment) {
    return paymentDetailRepository
        .findAllForPaymentHistory(
            payment.getId(),
            EntrantPaymentUpdateActor.LA,
            Arrays.asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK,
                InternalPaymentStatus.FAILED)
        );
  }
}