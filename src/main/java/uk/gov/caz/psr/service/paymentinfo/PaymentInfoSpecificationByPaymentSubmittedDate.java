package uk.gov.caz.psr.service.paymentinfo;

import java.time.ZoneId;
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
 * Creates specification to filter by payment made date.
 */
@Service
public class PaymentInfoSpecificationByPaymentSubmittedDate implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequestAttributes attributes) {
    return Optional.ofNullable(attributes.getPaymentSubmittedTimestamp()).isPresent();
  }

  @Override
  public Specification<EntrantPaymentMatchInfo> create(PaymentInfoRequestAttributes attributes) {
    return (root, criteriaQuery, criteriaBuilder) -> {

      Join<EntrantPaymentMatchInfo, PaymentInfo> joinPayment = QueryUtil
          .getOrCreateJoin(root, EntrantPaymentMatchInfo_.paymentInfo,
              QueryUtil.currentQueryIsCountRecords(criteriaQuery));

      return criteriaBuilder.and(
          criteriaBuilder.greaterThan(
              joinPayment.get(PaymentInfo_.submittedTimestamp),
              attributes.getPaymentSubmittedTimestamp().atStartOfDay(ZoneId.of("Europe/London"))
                  .withZoneSameInstant(ZoneId.of("GMT")).toLocalDateTime()
          ),
          criteriaBuilder.lessThan(
              joinPayment.get(PaymentInfo_.submittedTimestamp),
              attributes.getPaymentSubmittedTimestamp().plusDays(1)
                  .atStartOfDay(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneId.of("GMT"))
                  .toLocalDateTime()
          )
      );
    };
  }
}
