package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.CriteriaQuery;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;

/**
 * Interface of Payment info specification, used to group all specifications.
 */
public interface PaymentInfoSpecification {

  /**
   * Method used to verify if method create should be call.
   *
   * @param attributes {@link PaymentInfoRequestAttributes}
   * @return flag
   */
  boolean shouldUse(PaymentInfoRequestAttributes attributes);

  /**
   * Creates Specification object.
   *
   * @param attributes {@link PaymentInfoRequestAttributes}
   * @return {@link Specification}
   */
  Specification<EntrantPaymentMatchInfo> create(PaymentInfoRequestAttributes attributes);
}
