package uk.gov.caz.psr.service.paymentinfo;

import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Default Specification, used to init query.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CazIdSpecification implements Specification<VehicleEntrantPaymentInfo> {

  private final UUID cazId;

  @Override
  public Predicate toPredicate(Root<VehicleEntrantPaymentInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    criteriaQuery.distinct(true);
    QueryUtil.getOrCreateJoin(root, VehicleEntrantPaymentInfo_.paymentInfo);
    return criteriaBuilder.equal(root.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), cazId);
  }

  /**
   * Static factory method for creating {@link CazIdSpecification} instances with the passed
   * {@code cazId}.
   */
  public static CazIdSpecification forCaz(UUID cazId) {
    return new CazIdSpecification(cazId);
  }
}
