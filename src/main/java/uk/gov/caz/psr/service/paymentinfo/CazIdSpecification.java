package uk.gov.caz.psr.service.paymentinfo;

import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo_;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;

/**
 * Default Specification, used to init query.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CazIdSpecification implements Specification<EntrantPaymentMatchInfo> {

  private final UUID cazId;

  @Override
  public Predicate toPredicate(Root<EntrantPaymentMatchInfo> root, CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder) {
    QueryUtil.getOrCreateJoin(root, EntrantPaymentMatchInfo_.paymentInfo);
    Join<EntrantPaymentMatchInfo, EntrantPaymentInfo> entrantPaymentInfoJoin =
        QueryUtil.getOrCreateJoin(root, EntrantPaymentMatchInfo_.entrantPaymentInfo);
    return criteriaBuilder.and(
        criteriaBuilder.isTrue(root.get(EntrantPaymentMatchInfo_.latest)), // TODO extract
        criteriaBuilder.equal(
            entrantPaymentInfoJoin.get(EntrantPaymentInfo_.cleanAirZoneId),
            cazId
        ));
  }

  /**
   * Static factory method for creating {@link CazIdSpecification} instances with the passed
   * {@code cazId}.
   */
  public static CazIdSpecification forCaz(UUID cazId) {
    return new CazIdSpecification(cazId);
  }
}
