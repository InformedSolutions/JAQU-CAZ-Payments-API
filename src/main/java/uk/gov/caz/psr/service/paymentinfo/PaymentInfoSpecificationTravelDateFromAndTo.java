package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Creates Specification to filter by dates.
 */
@Service
public class PaymentInfoSpecificationTravelDateFromAndTo implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequestAttributes attributes) {
    return Optional.ofNullable(attributes.getFromDatePaidFor()).isPresent()
        && Optional.ofNullable(attributes.getToDatePaidFor()).isPresent();
  }

  @Override
  public Specification<VehicleEntrantPaymentInfo> create(
      PaymentInfoRequestAttributes attributes) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.greaterThanOrEqualTo(root.get(VehicleEntrantPaymentInfo_.travelDate),
            attributes.getFromDatePaidFor()),
        criteriaBuilder.lessThanOrEqualTo(root.get(VehicleEntrantPaymentInfo_.travelDate),
                attributes.getToDatePaidFor()));
  }
}
