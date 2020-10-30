package uk.gov.caz.psr.service.paymentinfo.byoperator;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.byoperator.PaymentInfoByOperator;

/**
 * Service class that retrieves paginated information about payments made by the given operator.
 */
@Service
@AllArgsConstructor
public class PaymentsInfoByOperatorService {

  private final PaginatedPaymentsInfoByOperatorFetcher infoFetcher;
  private final PaginatedPaymentsInfoByOperatorDetailsFetcher detailsFetcher;

  /**
   * Retrieves paginated information about payments made by the given operator identified by {@code
   * operatorId}.
   */
  public Page<PaymentInfoByOperator> getPaymentsByOperatorId(UUID operatorId, int pageSize,
      int pageNumber) {
    Page<PaymentInfo> paymentsByOperatorId = infoFetcher.getPaymentsByOperatorId(operatorId,
        pageSize, pageNumber);
    List<PaymentInfoByOperator> completePaymentInfos = detailsFetcher.fetchDetailsFor(
        paymentsByOperatorId.getContent());
    return new PageImpl<>(
        completePaymentInfos,
        paymentsByOperatorId.getPageable(),
        paymentsByOperatorId.getTotalElements()
    );
  }
}
