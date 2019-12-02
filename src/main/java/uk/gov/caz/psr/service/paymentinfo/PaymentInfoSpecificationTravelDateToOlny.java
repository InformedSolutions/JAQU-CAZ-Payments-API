package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Creates Specification to filter by to date only.
 */
@Service
public class PaymentInfoSpecificationTravelDateToOlny implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return !Optional.ofNullable(paymentInfoRequest.getFromDatePaidFor()).isPresent()
        && Optional.ofNullable(paymentInfoRequest.getToDatePaidFor()).isPresent();
  }

  @Override
  public Specification<VehicleEntrantPaymentInfo> create(PaymentInfoRequest paymentInfoRequest) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
        .equal(root.get(VehicleEntrantPaymentInfo_.travelDate),
            paymentInfoRequest.getToDatePaidFor().minusDays(1));
  }
}
