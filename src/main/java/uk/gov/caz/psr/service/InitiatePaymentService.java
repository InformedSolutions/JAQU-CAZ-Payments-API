package uk.gov.caz.psr.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.Payment;
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
    Optional<Payment> externalPaymentResult;

    paymentRepository.insert(payment);
    externalPaymentResult = externalPaymentsRepository.create(payment, request.getReturnUrl());

    externalPaymentResult.ifPresent(externalPayment -> {
      paymentRepository.update(externalPayment);
    });

    return payment;
  }

  /**
   * Builds Payment object based on request data.
   *
   * @param request A data which need to be used to create the payment.
   * @param correlationId request correlation_id.
   */
  private Payment buildPayment(InitiatePaymentRequest request, String correlationId) {
    return Payment.builder()
        .cleanZoneId(request.getCleanZoneId())
        .chargePaid(request.getAmount())
        .correlationId(correlationId)
        .build();
  }
}
