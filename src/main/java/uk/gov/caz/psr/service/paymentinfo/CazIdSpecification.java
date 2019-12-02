package uk.gov.caz.psr.service.paymentinfo;

import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Default Specification, used to init query.
 */
@AllArgsConstructor
public class CazIdSpecification implements Specification<VehicleEntrantPaymentInfo> {

  private UUID cazId;

  @Override
  public Predicate toPredicate(Root<VehicleEntrantPaymentInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    criteriaQuery.distinct(true);
    QueryUtil.getOrCreateJoin(root, criteriaQuery, VehicleEntrantPaymentInfo_.paymentInfo);
    return criteriaBuilder.equal(root.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), cazId);
  }
}
