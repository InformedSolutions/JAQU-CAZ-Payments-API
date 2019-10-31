package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a response from gov-uk pay.
 * See https://govukpay-api-browser.cloudapps.digital/#tocscreatepaymentresult for reference.
 */
@Value
@Builder
public class CreatePaymentResult {
  Integer amount;

  PaymentState state;

  @JsonProperty("payment_id")
  String paymentId;

  @JsonProperty("created_date")
  String createdDate;

  @JsonProperty("_links")
  PaymentLinks links;
}
