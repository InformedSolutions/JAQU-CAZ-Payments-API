package uk.gov.caz.psr.service.paymentinfo;

import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

@AllArgsConstructor
public class CazIdSpecification implements Specification<PaymentInfo> {

  private UUID cazId;

  @Override
  public Predicate toPredicate(Root<PaymentInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    criteriaQuery.distinct(true);
    Join<PaymentInfo, VehicleEntrantPaymentInfo> join = (Join) root
        .fetch(PaymentInfo_.vehicleEntrantPaymentInfoList);
    return criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), cazId);
  }
}
