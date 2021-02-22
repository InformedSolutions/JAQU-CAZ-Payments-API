package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo_;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;

/**
 * Creates Specification to filter by dates.
 */
@Service
public class PaymentInfoSpecificationTravelDateFromAndTo implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequestAttributes attributes) {
    return Optional.ofNullable(attributes.getFromDatePaidFor()).isPresent()
        && Optional.ofNullable(attributes.getToDatePaidFor()).isPresent();
  }

  @Override
  public Specification<EntrantPaymentMatchInfo> create(
      PaymentInfoRequestAttributes attributes) {
    return (root, criteriaQuery, criteriaBuilder) -> {


      Join<EntrantPaymentMatchInfo, EntrantPaymentInfo> entrantPaymentInfoJoin;
      if(currentQueryIsCountRecords(criteriaQuery)) {
        entrantPaymentInfoJoin = QueryUtil.getOrCreateJoin(root, EntrantPaymentMatchInfo_.entrantPaymentInfo);
      } else {
        entrantPaymentInfoJoin = QueryUtil.getOrCreateJoinFetch(root, EntrantPaymentMatchInfo_.entrantPaymentInfo);
      }


      return criteriaBuilder.and(
          criteriaBuilder.greaterThanOrEqualTo(
              entrantPaymentInfoJoin.get(EntrantPaymentInfo_.travelDate),
              attributes.getFromDatePaidFor()
          ),
          criteriaBuilder.lessThanOrEqualTo(
              entrantPaymentInfoJoin.get(EntrantPaymentInfo_.travelDate),
              attributes.getToDatePaidFor()
          )
      );
    };
  }

  private boolean currentQueryIsCountRecords(CriteriaQuery<?> criteriaQuery) {
    return criteriaQuery.getResultType() == Long.class || criteriaQuery.getResultType() == long.class;
  }
}
