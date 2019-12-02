package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Creates Specification to filter by vrn.
 */
@Service
public class PaymentInfoSpecificationVrn implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return Optional.ofNullable(paymentInfoRequest.getVrn()).isPresent();
  }

  @Override
  public Specification<VehicleEntrantPaymentInfo> create(PaymentInfoRequest paymentInfoRequest) {
    return (root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(VehicleEntrantPaymentInfo_.vrn),
            paymentInfoRequest.getVrn());
  }
}
