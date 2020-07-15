package uk.gov.caz.psr.service;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

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
}