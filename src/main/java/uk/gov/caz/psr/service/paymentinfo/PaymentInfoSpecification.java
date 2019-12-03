package uk.gov.caz.psr.service.paymentinfo;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;

/**
 * Interface of Payment info specification, used to group all specifications.
 */
public interface PaymentInfoSpecification {

  /**
   * Method used to verify if method create should be call.
   *
   * @param paymentInfoRequest {@link PaymentInfoRequest}
   * @return flag
   */
  boolean shouldUse(PaymentInfoRequest paymentInfoRequest);

  /**
   * Creates Specification object.
   *
   * @param paymentInfoRequest {@link PaymentInfoRequest}
   * @return {@link Specification}
   */
  Specification<VehicleEntrantPaymentInfo> create(PaymentInfoRequest paymentInfoRequest);
}
