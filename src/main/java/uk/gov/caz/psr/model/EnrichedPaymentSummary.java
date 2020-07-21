package uk.gov.caz.psr.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that enriches {@link PaymentSummary} with data coming from Accounts API and VCCS API.
 */
@Value
@Builder
public class EnrichedPaymentSummary {

  /**
   * Id of the payment.
   */
  UUID paymentId;

  /**
   * Quantity of the payed entries to CAZ.
   */
  int entriesCount;

  /**
   * Total amount paid.
   */
  int totalPaid;

  /**
   * Name of the Clean Air Zone.
   */
  String cazName;

  /**
   * Name of the payer.
   */
  String payerName;
}
