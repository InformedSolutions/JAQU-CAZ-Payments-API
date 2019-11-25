package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.psr.model.ExternalPaymentStatus;

/**
 * Value object representing a response from gov-uk pay. See https://govukpay-api-browser.cloudapps.digital/#tocsgetpaymentresult
 * for reference.
 */
@Value
@Builder
@Slf4j
public class GetPaymentResult {

  int amount;
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
  PaymentState state;

  /**
   * Converts the external status to its representation in model. If the status is not recognized,
   * {@link ExternalPaymentStatus#UNKNOWN} is returned.
   */
  public ExternalPaymentStatus getPaymentStatus() {
    String status = state.getStatus();
    try {
      return ExternalPaymentStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Unrecognized payment status '{}', returning {}", status,
          ExternalPaymentStatus.UNKNOWN);
      return ExternalPaymentStatus.UNKNOWN;
    }
  }
}

