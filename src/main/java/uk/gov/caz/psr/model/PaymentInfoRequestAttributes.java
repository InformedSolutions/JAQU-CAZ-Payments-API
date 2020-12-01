package uk.gov.caz.psr.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * A class which holds attributes related for handling payment-info request.
 */
@Builder
@Value
public class PaymentInfoRequestAttributes {
  String externalPaymentId;

  String vrn;

  LocalDate fromDatePaidFor;

  LocalDate toDatePaidFor;

  LocalDate paymentSubmittedTimestamp;
}
