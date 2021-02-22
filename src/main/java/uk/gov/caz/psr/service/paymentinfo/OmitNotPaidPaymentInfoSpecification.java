package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo_;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;

/**
 * A specification that does not include vehicle entrant payments with {@code notPaid} status.
 */
public class OmitNotPaidPaymentInfoSpecification implements Specification<EntrantPaymentMatchInfo> {

  @Override
  public Predicate toPredicate(Root<EntrantPaymentMatchInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {

    Join<EntrantPaymentMatchInfo, EntrantPaymentInfo> entrantPaymentInfoJoin;
    if (QueryUtil.currentQueryIsCountRecords(criteriaQuery)) {
      entrantPaymentInfoJoin = QueryUtil.getOrCreateJoin(root,
          EntrantPaymentMatchInfo_.entrantPaymentInfo);
    } else {
      entrantPaymentInfoJoin = QueryUtil.getOrCreateJoinFetch(root,
          EntrantPaymentMatchInfo_.entrantPaymentInfo);
    }
    return criteriaBuilder.notEqual(
        entrantPaymentInfoJoin.get(EntrantPaymentInfo_.paymentStatus),
        InternalPaymentStatus.NOT_PAID);
  }
  
}
