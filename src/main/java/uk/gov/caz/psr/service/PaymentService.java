package uk.gov.caz.psr.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.service.exception.PaymentNotFoundException;
import uk.gov.caz.psr.util.ReferencesHistoryConverter;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentDetailRepository paymentDetailRepository;
  private final ReferencesHistoryConverter referencesHistoryConverter;

  /**
   * Finds a given payment by its internal identifier passed as {@code paymentId}.
   *
   * @param paymentId An internal identifier of the payment.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   */
  public Optional<Payment> getPayment(UUID paymentId) {
    return paymentRepository.findById(paymentId);
  }

  /**
   * Finds a given payment by central reference number passed as {@code referenceNumber}.
   *
   * @param referenceNumber central reference number.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException if {@code referenceNumber} is null
   */
  public ReferencesHistoryResponse getPaymentHistoryByReferenceNumber(Long referenceNumber) {
    Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
        .orElseThrow(() -> new PaymentNotFoundException(
            "Payment with provided reference number does not exist"));

    List<PaymentModification> paymentModifications = paymentDetailRepository
        .findAllForPaymentHistory(
            payment.getId(),
            EntrantPaymentUpdateActor.LA,
            Arrays.asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK));

    return referencesHistoryConverter.toReferencesHistoryResponse(payment, paymentModifications);
  }
}