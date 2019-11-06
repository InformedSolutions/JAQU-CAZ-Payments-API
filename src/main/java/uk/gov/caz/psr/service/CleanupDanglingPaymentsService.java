package uk.gov.caz.psr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for updating statuses of old, unfinished payments.
 */
@Service
@Slf4j
public class CleanupDanglingPaymentsService {

  /**
   * Finds old and unfinished payments in the database, checks their statuses in the gov uk pay
   * service and updates them in the database accordingly.
   */
  public void updateStatusesOfDanglingPayments() {
    log.info("Cleaning up dangling payments - start");

    // TODO to be implemented in CAZ-1345

    log.info("Cleaning up dangling payments - finish");
  }
}
