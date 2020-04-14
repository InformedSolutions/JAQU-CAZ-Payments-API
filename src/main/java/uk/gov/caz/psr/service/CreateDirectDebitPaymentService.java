package uk.gov.caz.psr.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.dto.external.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.SingleEntrantPayment;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentStatusUpdater;

/**
 * A service which is responsible creating DirectDebit Payment in PaymentProvider.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CreateDirectDebitPaymentService {

  private final PaymentRepository paymentRepository;
  private final InitiateEntrantPaymentsService initiateEntrantPaymentsService;
  private final ExternalDirectDebitRepository externalDirectDebitRepository;
  private final DirectDebitPaymentStatusUpdater directDebitPaymentStatusUpdater;

  /**
   * Creates Payment in Payment Provider, inserts Payment details into database.
   */
  @Transactional
  public Payment createPayment(Payment payment, List<SingleEntrantPayment> entrantPayments) {
    try {
      log.info("Create DirectPayment process: start");
      Payment paymentWithInternalId = initializePaymentWithEntrants(payment, entrantPayments);

      DirectDebitPayment directDebitPayment = externalDirectDebitRepository
          .collectPayment(paymentWithInternalId.getPaymentProviderMandateId(),
              paymentWithInternalId.getTotalPaid(),
              paymentWithInternalId.getReferenceNumber().toString(),
              payment.getCleanAirZoneId());

      return directDebitPaymentStatusUpdater
          .updateWithDirectDebitPaymentDetails(paymentWithInternalId, directDebitPayment);
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
