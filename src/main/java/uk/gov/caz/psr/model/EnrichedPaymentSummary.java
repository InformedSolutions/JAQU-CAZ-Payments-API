package uk.gov.caz.psr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  BigDecimal totalPaid;

  /**
   * Name of the Clean Air Zone.
   */
  String cazName;

  /**
   * Name of the payer.
   */
  String payerName;

  /**
   * Date of the payment.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate paymentDate;
}
