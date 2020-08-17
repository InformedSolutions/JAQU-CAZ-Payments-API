package uk.gov.caz.psr.util;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorResponse;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorResponse.SinglePaymentsInfoByOperator;
import uk.gov.caz.psr.model.info.byoperator.PaymentInfoByOperator;

/**
 * Utility class containing methods that transform {@link Page} to {@link
 * PaymentsInfoByOperatorResponse}.
 */
@UtilityClass
public class PaymentInfoByOperatorIdConverter {

  /**
   * Converts {@link Page} to {@link PaymentsInfoByOperatorResponse}.
   */
  public static PaymentsInfoByOperatorResponse from(Page<PaymentInfoByOperator> page,
      int pageSize) {
    return PaymentsInfoByOperatorResponse.builder()
        .page(page.getNumber())
        .pageCount(page.getTotalPages())
        .perPage(pageSize)
        .totalPaymentsCount(page.getTotalElements())
        .payments(PaymentInfoByOperatorIdConverter.from(page.getContent()))
        .build();
  }

  /**
   * Converts a list of {@link PaymentInfoByOperator} to a list of {@link
   * SinglePaymentsInfoByOperator}.
   */
  private static List<SinglePaymentsInfoByOperator> from(List<PaymentInfoByOperator> paymentsInfo) {
    return paymentsInfo.stream()
        .map(paymentInfo -> SinglePaymentsInfoByOperator.builder()
            .paymentTimestamp(paymentInfo.getPaymentTimestamp())
            .cazName(paymentInfo.getCazName())
            .totalPaid(paymentInfo.getTotalPaid())
            .paymentId(paymentInfo.getPaymentId())
            .paymentReference(paymentInfo.getPaymentReference())
            .vrns(paymentInfo.getVrns())
            .paymentProviderStatus(paymentInfo.getPaymentProviderStatus())
            .build()
        )
        .collect(Collectors.toList());
  }
}
