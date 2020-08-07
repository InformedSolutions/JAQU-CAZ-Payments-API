package uk.gov.caz.psr.util;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;

/**
 * Converters DTO to model for {@link InitiatePaymentRequest}.
 */
@UtilityClass
public class InitiatePaymentRequestToModelConverter {

  /**
   * Builds Payment based on request data.
   *
   * @param request A data which need to be used to create the payment.
   */
  public static Payment toPayment(InitiatePaymentRequest request) {
    return Payment.builder()
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .totalPaid(calculateTotal(request.getTransactions()))
        .entrantPayments(Collections.emptyList())
        .cleanAirZoneId(request.getCleanAirZoneId())
        .userId(toUuidIfPresent(request.getUserId()))
        .operatorId(toUuidIfPresent(request.getOperatorId()))
        .telephonePayment(request.getTelephonePayment())
        .build();
  }

  /**
   * Converts the passed identifier to {@link UUID} provided it contains a non-empty text.
   */
  private static UUID toUuidIfPresent(String identifier) {
    return StringUtils.hasText(identifier) ? UUID.fromString(identifier) : null;
  }

  /**
   * Calculates the total, i.e. the amount which needs to be paid for all {@code transactions}.
   */
  private static int calculateTotal(List<Transaction> transactions) {
    return transactions
        .stream()
        .mapToInt(Transaction::getCharge)
        .sum();
  }
}
