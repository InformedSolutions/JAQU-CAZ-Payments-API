package uk.gov.caz.psr.service;

import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * Service responsible for updating statuses of old, unfinished payments.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CleanupDanglingPaymentsService {

  private final PaymentRepository paymentRepository;
  private final CleanupDanglingPaymentService cleanupDanglingPaymentService;

  /**
   * Finds old and unfinished payments in the database, checks their statuses in the gov uk pay
   * service and updates them in the database accordingly.
   */
  public void updateStatusesOfDanglingPayments() {
    log.info("Cleaning up dangling payments - start");
    Stopwatch stopwatch = Stopwatch.createStarted();
    List<Payment> danglingPayments = paymentRepository.findDanglingPayments();
    log.info("Found {} dangling payments", danglingPayments.size());
    for (Payment danglingPayment : danglingPayments) {
      processDanglingPayment(danglingPayment);
    }
    log.info("Cleaning up dangling payments - finish, the execution took {}ms",
        stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  private void processDanglingPayment(Payment danglingPayment) {
    try {
      cleanupDanglingPaymentService.processDanglingPayment(danglingPayment);
    } catch (Exception e) {
      log.error("Error while processing the dangling payment with id '{}'", danglingPayment.getId(),
          e);
    }
  }
}
