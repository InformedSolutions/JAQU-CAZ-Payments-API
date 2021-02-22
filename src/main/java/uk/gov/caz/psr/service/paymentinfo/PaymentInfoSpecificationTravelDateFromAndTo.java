package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
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

      Join<EntrantPaymentMatchInfo, EntrantPaymentInfo> entrantPaymentInfoJoin = QueryUtil
          .getOrCreateJoin(root, EntrantPaymentMatchInfo_.entrantPaymentInfo,
              QueryUtil.currentQueryIsCountRecords(criteriaQuery));

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
}
