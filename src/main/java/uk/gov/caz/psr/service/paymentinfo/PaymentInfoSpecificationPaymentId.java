package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import javax.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Creates Specification to filter by payment id.
 */
@Service
public class PaymentInfoSpecificationPaymentId implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return Optional.ofNullable(paymentInfoRequest.getPaymentProviderId()).isPresent();
  }

  @Override
  public Specification<VehicleEntrantPaymentInfo> create(PaymentInfoRequest paymentInfoRequest) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      criteriaQuery.distinct(true);
      Join<VehicleEntrantPaymentInfo, PaymentInfo> join = QueryUtil
          .getOrCreateJoin(root, criteriaQuery, VehicleEntrantPaymentInfo_.paymentInfo);
      return criteriaBuilder
          .equal(join.get(PaymentInfo_.externalId), paymentInfoRequest.getPaymentProviderId());
    };
  }
}
