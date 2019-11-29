package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;

@Service
public class PaymentInfoSpecificationPaymentId implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return Optional.ofNullable(paymentInfoRequest.getPaymentId()).isPresent();
  }

  @Override
  public Specification<PaymentInfo> create(PaymentInfoRequest paymentInfoRequest) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      criteriaQuery.distinct(true);
      return criteriaBuilder
          .equal(root.get(PaymentInfo_.externalId), paymentInfoRequest.getPaymentId());
    };
  }
}
