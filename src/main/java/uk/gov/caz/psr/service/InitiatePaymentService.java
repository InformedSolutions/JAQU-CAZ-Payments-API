
package uk.gov.caz.psr.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.SingleEntrantPayment;
import uk.gov.caz.psr.repository.ExternalCardPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
public class InitiatePaymentService {

  private final ExternalCardPaymentsRepository externalCardPaymentsRepository;
  private final PaymentRepository paymentRepository;
  private final InitiateEntrantPaymentsService initiateEntrantPaymentsService;

  /**
   * Creates Payment in GOV.UK PAY, inserts Payment details into database.
   */
  @Transactional
  public Payment createPayment(Payment payment, List<SingleEntrantPayment> entrantPayments,
      String returnUrl) {
    Payment paymentWithInternalId = paymentRepository.insert(payment);
    Payment paymentWithExternalId = externalCardPaymentsRepository.create(paymentWithInternalId,
        returnUrl);
    initiateEntrantPaymentsService.processEntrantPaymentsForPayment(paymentWithInternalId.getId(),
        payment.getCleanAirZoneId(), entrantPayments);
    paymentRepository.update(paymentWithExternalId);
    return paymentWithExternalId;
  }
}
