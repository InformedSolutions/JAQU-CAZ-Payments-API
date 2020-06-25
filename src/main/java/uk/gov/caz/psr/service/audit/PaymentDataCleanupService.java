package uk.gov.caz.psr.service.audit;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.audit.PaymentLoggedActionRepository;

/**
 * Service class that remove unidentificable vehicle data
 * as per privacy statement.
 */
@Service
@Slf4j
public class PaymentDataCleanupService {
  
  private final PaymentLoggedActionRepository paymentLoggedActionRepository;
  private final PaymentDetailRepository paymentDetailRepository;
  private final int paymentDetailCleanupMonths;
  private final int paymentLoggedActionCleanupMonths;

  /**
   * Class constructor.
   * @param paymentLoggedActionRepository instance of {@PaymentLoggedActionRepository}
   * @param paymentDetailRepository instance of {@PaymentDetailRepository}
   * @param paymentDetailCleanupMonths payment details record age in months
   * @param paymentLoggedActionCleanupMonths logged action record age in months
   */
  public PaymentDataCleanupService(
      PaymentLoggedActionRepository paymentLoggedActionRepository,
      PaymentDetailRepository paymentDetailRepository,
      @Value("${services.audit.payment-detail-cleanup:18}") int paymentDetailCleanupMonths,
      @Value("${services.audit.payment-logged-action-cleanup:18}")
      int paymentLoggedActionCleanupMonths) {
    this.paymentLoggedActionRepository = paymentLoggedActionRepository;
    this.paymentDetailRepository = paymentDetailRepository;
    this.paymentDetailCleanupMonths = paymentDetailCleanupMonths;
    this.paymentLoggedActionCleanupMonths = paymentLoggedActionCleanupMonths;
  }

  /**
   * clean up old log data before a given date.
   */
  public void cleanupData() {
    try {
      cleanupPaymentAuditData();
      cleanupLoggedActionAuditData();
      log.info("PaymentAuditDataCleanupService cleanup finished sucessfully");
    } catch (Exception ex) {
      log.info("PaymentAuditDataCleanupService cleanup failed due to {}", ex.getMessage());
      throw ex;
    }
  }

  /**
   * clean up payment detail log records.
   */
  private void cleanupPaymentAuditData() {
    log.info("PaymentAuditDataCleanupService start cleaning up"
        + " payment log records older than {} months", paymentDetailCleanupMonths);
    paymentDetailRepository.deleteLogsBeforeDate(
        LocalDate.now().minusMonths(paymentDetailCleanupMonths));
  }

  /**
   * clean up logged action records.
   */
  private void cleanupLoggedActionAuditData() {
    log.info("PaymentAuditDataCleanupService start cleaning up"
        + " logged action records older than {} months", paymentLoggedActionCleanupMonths);
    paymentLoggedActionRepository.deleteLogsBeforeDate(
        LocalDate.now().minusMonths(paymentLoggedActionCleanupMonths));
  }
}