package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;

/**
 * Specification that narrows down {@link EntrantPaymentMatchInfo} records to the ones with
 * {@link EntrantPaymentMatchInfo#isLatest()} equal to {@code true}.
 */
public class LatestPaymentInfoSpecification implements Specification<EntrantPaymentMatchInfo> {

  @Override
  public Predicate toPredicate(Root<EntrantPaymentMatchInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    return criteriaBuilder.isTrue(root.get(EntrantPaymentMatchInfo_.latest));
  }
}
