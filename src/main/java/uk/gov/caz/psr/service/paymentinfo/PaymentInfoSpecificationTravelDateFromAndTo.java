package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;

/**
 * Creates Specification to filter by dates.
 */
@Service
public class PaymentInfoSpecificationTravelDateFromAndTo implements PaymentInfoSpecification {

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return Optional.ofNullable(paymentInfoRequest.getFromDatePaidFor()).isPresent()
        && Optional.ofNullable(paymentInfoRequest.getToDatePaidFor()).isPresent();
  }

  @Override
  public Specification<VehicleEntrantPaymentInfo> create(PaymentInfoRequest paymentInfoRequest) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.greaterThanOrEqualTo(root.get(VehicleEntrantPaymentInfo_.travelDate),
            paymentInfoRequest.getFromDatePaidFor()),
        criteriaBuilder
            .lessThanOrEqualTo(root.get(VehicleEntrantPaymentInfo_.travelDate),
                paymentInfoRequest.getToDatePaidFor()));
  }
}
