package uk.gov.caz.psr.dto.validation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.PaymentInfoRequestV1;

@Component
public class MaximumDateQueryRangeValidator {

  private final int maxDateQueryRange;

  /**
   * Default constructor.
   */
  public MaximumDateQueryRangeValidator(
      @Value("${api.charge-settlement.date-query-range}") int maxDateQueryRange) {
    this.maxDateQueryRange = maxDateQueryRange;
  }

  /**
   * Method which validate maximum date range query.
   */
  public void validateDateRange(PaymentInfoRequestV1 paymentInfoRequestV1) {
    LocalDate fromDatePaidFor = paymentInfoRequestV1.getFromDatePaidFor();
    LocalDate toDatePaidFor = paymentInfoRequestV1.getToDatePaidFor();
    if (fromDatePaidFor == null || toDatePaidFor == null) {
      return;
    }
    if (checkMaxDateQueryRange(fromDatePaidFor, toDatePaidFor)) {
      throw new PaymentInfoMaxDateRangeValidationException("max date query range exceeded");
    }
  }

  private boolean checkMaxDateQueryRange(LocalDate fromDatePaidFor, LocalDate toDatePaidFor) {
    return fromDatePaidFor.plus(maxDateQueryRange, ChronoUnit.DAYS).isBefore(toDatePaidFor);
  }
}