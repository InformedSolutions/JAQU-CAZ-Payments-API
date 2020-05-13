package uk.gov.caz.psr.util;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.model.SingleEntrantPayment;

/**
 * Converters Transactions list DTO to model.
 */
@UtilityClass
public class PaymentTransactionsToEntrantsConverter {

  /**
   * Converts a list of {@link Transaction}s to its model counterpart, a list of {@link
   * SingleEntrantPayment}s.
   */
  public static List<SingleEntrantPayment> toSingleEntrantPayments(
      List<Transaction> transactions) {
    return transactions
        .stream()
        .map(transaction -> SingleEntrantPayment.builder()
            .charge(transaction.getCharge())
            .tariffCode(transaction.getTariffCode())
            .travelDate(transaction.getTravelDate())
            .vrn(transaction.getVrn())
            .build()
        ).collect(Collectors.toList());
  }

}
