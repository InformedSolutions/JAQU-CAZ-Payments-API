package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * A specification that does not include vehicle entrant payments with {@code notPaid} status.
 */
public class OmitNotPaidPaymentInfoSpecification
    implements Specification<VehicleEntrantPaymentInfo> {

  @Override
  public Predicate toPredicate(Root<VehicleEntrantPaymentInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    return criteriaBuilder.notEqual(root.get(VehicleEntrantPaymentInfo_.paymentStatus),
        InternalPaymentStatus.NOT_PAID);
  }
}
