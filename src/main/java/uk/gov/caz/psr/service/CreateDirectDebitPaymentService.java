package uk.gov.caz.psr.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.SingleEntrantPayment;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentFinalizer;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentService;

/**
 * A service which is responsible creating DirectDebit Payment in PaymentProvider.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CreateDirectDebitPaymentService {

  private final PaymentRepository paymentRepository;
  private final InitiateEntrantPaymentsService initiateEntrantPaymentsService;
  private final DirectDebitPaymentFinalizer directDebitPaymentFinalizer;
  private final DirectDebitPaymentService directDebitPaymentService;

  /**
   * Creates Payment in Payment Provider, inserts Payment details into database.
   */
  @Transactional
  public Payment createPayment(Payment payment, List<SingleEntrantPayment> entrantPayments) {
    try {
      log.info("Create DirectPayment process: start");
      Payment paymentWithInternalId = initializePaymentWithEntrants(payment, entrantPayments);
      DirectDebitPayment directDebitPayment = directDebitPaymentService
          .collectPayment(paymentWithInternalId.getId(), payment.getCleanAirZoneId(),
              paymentWithInternalId.getTotalPaid(), paymentWithInternalId.getReferenceNumber(),
              paymentWithInternalId.getPaymentProviderMandateId());

      Payment paymentWithExternalId = directDebitPaymentFinalizer
          .finalizeSuccessfulPayment(paymentWithInternalId,
              directDebitPayment.getPaymentId(),
              payment.getEmailAddress());
      return paymentWithExternalId;
    } finally {
      log.info("Create DirectPayment process: finish");
    }
  }

  /**
   * Create Payment and EntrantPayments in DB.
   */
  private Payment initializePaymentWithEntrants(Payment payment,
      List<SingleEntrantPayment> entrantPayments) {
    Payment paymentWithInternalId = paymentRepository.insert(payment);
    initiateEntrantPaymentsService.processEntrantPaymentsForPayment(paymentWithInternalId.getId(),
        payment.getCleanAirZoneId(), entrantPayments);

    // Trying to get created payment with matched entrant payments.
    return paymentRepository.findById(paymentWithInternalId.getId())
        .orElseThrow(() -> new RuntimeException("Payment initialization failed"));
  }
}
