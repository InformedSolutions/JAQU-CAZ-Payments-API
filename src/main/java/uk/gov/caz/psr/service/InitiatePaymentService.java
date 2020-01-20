
package uk.gov.caz.psr.service;

import java.util.Collections;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
public class InitiatePaymentService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository paymentRepository;

  private final InitiateEntrantPaymentsService initiateEntrantPaymentsService;

  /**
   * Creates Payment in GOV.UK PAY Inserts Payment details into database.
   *
   * @param request A data which need to be used to create the payment.
   */
  @Transactional
  public Payment createPayment(InitiatePaymentRequest request) {
    Payment payment = buildPayment(request);

    Payment paymentWithInternalId = paymentRepository.insert(payment);
    Payment paymentWithExternalId = externalPaymentsRepository.create(paymentWithInternalId,
        request.getReturnUrl());
    initiateEntrantPaymentsService.processEntrantPaymentsForPayment(paymentWithInternalId.getId(),
        request.getAmount(), request.getDays(), request.getTariffCode(), request.getVrn(),
        request.getCleanAirZoneId()
    );

    paymentRepository.update(paymentWithExternalId);
    return paymentWithExternalId;
  }

  /**
   * Builds Payment object without based on request data.
   *
   * @param request A data which need to be used to create the payment.
   */

  private Payment buildPayment(InitiatePaymentRequest request) {
    return Payment.builder()
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .totalPaid(request.getAmount())
        .entrantPayments(Collections.emptyList())
        .cleanAirZoneId(request.getCleanAirZoneId())
        .build();
  }
}
