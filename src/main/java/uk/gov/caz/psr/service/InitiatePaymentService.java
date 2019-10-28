package uk.gov.caz.psr.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
public class InitiatePaymentService {

  private final PaymentRepository paymentRepository;
  private final ExternalPaymentsRepository externalPaymentsRepository;

  /**
   * Creates Payment in GOV.UK PAY Inserts Payment details into database
   *
   * @param request A data which need to be used to create the payment.
   * @param correlationId request correlation_id.
   */
  public Payment createPayment(InitiatePaymentRequest request, String correlationId) {
    Payment payment = buildPayment(request, correlationId);
    Payment paymentWithInternalId = paymentRepository.insert(payment);
    // TODO add record(s) to vehicle_entrant_payment table
    Payment paymentWithExternalId = externalPaymentsRepository.create(paymentWithInternalId,
        request.getReturnUrl());
    paymentRepository.update(paymentWithExternalId);
    return paymentWithExternalId;
  }

  /**
   * Builds Payment object based on request data.
   *
   * @param request A data which need to be used to create the payment.
   * @param correlationId request correlation_id.
   */
  private Payment buildPayment(InitiatePaymentRequest request, String correlationId) {
    return Payment.builder()
        .status(PaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .cleanZoneId(request.getCleanZoneId())
        .chargePaid(request.getAmount())
        .correlationId(correlationId)
        .build();
  }
}
