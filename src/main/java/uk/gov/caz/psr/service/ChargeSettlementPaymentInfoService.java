package uk.gov.caz.psr.service;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.repository.jpa.VehicleEntrantPaymentInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.CazIdSpecification;
import uk.gov.caz.psr.service.paymentinfo.OmitNotPaidPaymentInfoSpecification;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoSpecification;

/**
 * Service which merge specification and run query.
 */
@Service
@AllArgsConstructor
public class ChargeSettlementPaymentInfoService {

  private final VehicleEntrantPaymentInfoRepository vehicleEntrantPaymentInfoRepository;

  private final List<PaymentInfoSpecification> specifications;

  /**
   * Method which filter payments based on PaymentInfoRequest.
   *
   * @param attributes {@link PaymentInfoRequestAttributes}
   * @param cazId for payment
   * @return  list of {@link PaymentInfo}
   */
  public List<VehicleEntrantPaymentInfo> findPaymentInfo(PaymentInfoRequestAttributes attributes,
      UUID cazId) {
    Specification<VehicleEntrantPaymentInfo> specification = specifications.stream()
        .filter(paymentInfoSpecification -> paymentInfoSpecification.shouldUse(attributes))
        .map(paymentInfoSpecification -> paymentInfoSpecification.create(attributes))
        .reduce(initialSpecification(cazId), Specification::and);

    return vehicleEntrantPaymentInfoRepository.findAll(specification);
  }

  /**
   * Returns the initial specification that selects entries for a given {@code cazId} and
   * does not include payments with {@code notPaid} status.
   */
  private Specification<VehicleEntrantPaymentInfo> initialSpecification(UUID cazId) {
    return CazIdSpecification.forCaz(cazId).and(new OmitNotPaidPaymentInfoSpecification());
  }
}
