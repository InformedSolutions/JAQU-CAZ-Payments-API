package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a response from gov-uk pay.
 * See https://govukpay-api-browser.cloudapps.digital/#tocsgetpaymentresult for reference.
 */
@Value
@Builder
public class GetPaymentResult {
  Integer amount;
  String description;
  String reference;
  String language;
  Map<String, String> metadata;
  String email;
  @JsonProperty("payment_id")
  String paymentId;
  @JsonProperty("payment_provider")
  String paymentProvider;
  @JsonProperty("created_date")
  String createdDate;
  @JsonProperty("total_amount")
  int totalAmount;
  @JsonProperty("return_url")
  String returnUrl;
}
