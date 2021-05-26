package uk.gov.caz.psr.service.paymentinfo.bydates;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;
import uk.gov.caz.psr.repository.jpa.PaymentInfoRepository;

/**
 * Service class that retrieves paginated information from the T_PAYMENT table about payments made
 * by the given operator.
 */
@Service
@AllArgsConstructor
class PaginatedPaymentsInfoByDatesFetcher {

  private final PaymentInfoRepository paymentInfoRepository;

  /**
   * Retrieves paginated information from the T_PAYMENT table about payments made by the given
   * operator.
   */
  public Page<PaymentInfo> getPaymentsByDates(int pageSize, int pageNumber,
      LocalDateTime startDate, LocalDateTime endDate) {
    Specification<PaymentInfo> byDates = (
        root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.and(
            criteriaBuilder
                .greaterThanOrEqualTo(root.get(PaymentInfo_.INSERT_TIMESTAMP), startDate),
            criteriaBuilder.lessThan(root.get(PaymentInfo_.INSERT_TIMESTAMP), endDate));

    return paymentInfoRepository.findAll(byDates, buildPageRequest(pageSize, pageNumber));
  }

  /**
   * Creates {@link PageRequest} based on the provided arguments.
   */
  private PageRequest buildPageRequest(int pageSize, int pageNumber) {
    return PageRequest.of(
        pageNumber,
        pageSize,
        Sort.by(Order.desc("insertTimestamp"))
    );
  }
}
