package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import javax.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;

/**
 * Creates Specification to filter by payment id.
 */
@Service
public class PaymentInfoSpecificationPaymentId implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequestAttributes attributes) {
    return Optional.ofNullable(attributes.getExternalPaymentId()).isPresent();
  }

  @Override
  public Specification<EntrantPaymentMatchInfo> create(PaymentInfoRequestAttributes attributes) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      Join<EntrantPaymentMatchInfo, PaymentInfo> joinPayment =
          QueryUtil.getOrCreateJoinFetch(root, EntrantPaymentMatchInfo_.paymentInfo);
      return criteriaBuilder.equal(
          joinPayment.get(PaymentInfo_.externalId),
          attributes.getExternalPaymentId()
      );
    };
  }
}
