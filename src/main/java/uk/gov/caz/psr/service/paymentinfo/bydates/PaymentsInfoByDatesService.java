package uk.gov.caz.psr.service.paymentinfo.bydates;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.byoperator.PaymentInfoByOperator;
import uk.gov.caz.psr.service.paymentinfo.byoperator.PaginatedPaymentsInfoByOperatorDetailsFetcher;

/**
 * Service class that retrieves paginated information about payments made by the given operator.
 */
@Service
@AllArgsConstructor
public class PaymentsInfoByDatesService {

  private final PaginatedPaymentsInfoByDatesFetcher infoFetcher;
  public final PaginatedPaymentsInfoByOperatorDetailsFetcher detailsFetcher;

  /**
   * Retrieves paginated information about payments made by the given operator identified by {@code
   * operatorId}.
   */
  public Page<PaymentInfoByOperator> getPaymentsByDates(int pageSize,
      int pageNumber, LocalDateTime startDate, LocalDateTime endDate) {
    Page<PaymentInfo> paymentsByOperatorId = infoFetcher.getPaymentsByDates(
        pageSize, pageNumber, startDate, endDate);
    List<PaymentInfoByOperator> completePaymentInfos = detailsFetcher.fetchDetailsFor(
        paymentsByOperatorId.getContent());
    return new PageImpl<>(
        completePaymentInfos,
        paymentsByOperatorId.getPageable(),
        paymentsByOperatorId.getTotalElements()
    );
  }
}
