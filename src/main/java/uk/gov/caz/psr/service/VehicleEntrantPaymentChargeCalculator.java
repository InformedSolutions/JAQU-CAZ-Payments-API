package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * A class that calculates a charge per individual date based on the total amount paid.
 */
@Service
@Slf4j
public class VehicleEntrantPaymentChargeCalculator {

  /**
   * Calculates the charge per day based on the total amount paid and the number of days against
   * which the payment is made.
   *
   * @param total The total amount to be paid.
   * @param numberOfDays The number of days against which the payment is made.
   * @return The charge per day.
   */
  public int calculateCharge(int total, int numberOfDays) {
    Preconditions.checkArgument(total > 0, "Expecting 'total' (%s) to be a positive number", total);
    Preconditions.checkArgument(numberOfDays > 0, "Expecting 'numberOfDays' (%s) to "
        + "be a positive number", numberOfDays);

    if (total % numberOfDays != 0) {
      log.warn("'total' / 'numberOfDays' ({}/{}) is not a natural number, this should only happen "
              + "for discounted entrants. Current value: {}", total, numberOfDays,
          (double) total / numberOfDays);
    }
    return total / numberOfDays;
  }
}
