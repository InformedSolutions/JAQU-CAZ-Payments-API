package uk.gov.caz.psr.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

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
   * Converts this object to an instance of {@link Payment}.
   */
  public Payment toPayment() {
    return Payment.builder()
        .externalPaymentId(paymentId)
        .status(getPaymentStatus())
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .chargePaid(amount)
        .build();
  }

  /**
   * Converts the external status to its representation in model. If the status is not recognized,
   * {@link PaymentStatus#UNKNOWN} is returned.
   */
  private PaymentStatus getPaymentStatus() {
    String status = state.getStatus();
    try {
      return PaymentStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Unrecognized payment status '{}', returning {}", status, PaymentStatus.UNKNOWN);
      return PaymentStatus.UNKNOWN;
    }
  }
}

