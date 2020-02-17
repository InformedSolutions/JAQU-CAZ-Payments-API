package uk.gov.caz.psr.util;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentRequest.Transaction;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.SingleEntrantPayment;

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
        .userId(StringUtils.hasText(request.getUserId())
            ? UUID.fromString(request.getUserId())
            : null)
        .build();
  }

  /**
   * Converts a list of {@link Transaction}s to its model counterpart, a list of {@link
   * SingleEntrantPayment}s.
   */
  public static List<SingleEntrantPayment> toSingleEntrantPayments(
      InitiatePaymentRequest request) {
    return request.getTransactions()
        .stream()
        .map(transaction -> SingleEntrantPayment.builder()
            .charge(transaction.getCharge())
            .tariffCode(transaction.getTariffCode())
            .travelDate(transaction.getTravelDate())
            .vrn(transaction.getVrn())
            .build()
        ).collect(Collectors.toList());
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
