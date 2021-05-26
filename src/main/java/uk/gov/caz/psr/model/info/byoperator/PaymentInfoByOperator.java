package uk.gov.caz.psr.model.info.byoperator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Helper value object that stores information about a payment made by an operator.
 */
@Value
@Builder
public class PaymentInfoByOperator {

  String cazName;

  int totalPaid;

  UUID paymentId;

  UUID operatorId;

  long paymentReference;

  Set<String> vrns;

  String paymentProviderStatus;

  LocalDateTime paymentTimestamp;

  boolean isRefunded;

  boolean isChargedback;
}
