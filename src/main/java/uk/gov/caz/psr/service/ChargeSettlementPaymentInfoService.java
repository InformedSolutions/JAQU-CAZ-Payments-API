package uk.gov.caz.psr.service;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.repository.jpa.PaymentInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.CazIdSpecification;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoSpecification;

/**
 * Service which merge specification and run query.
 */
@Service
@AllArgsConstructor
public class ChargeSettlementPaymentInfoService {

  private PaymentInfoRepository paymentInfoRepository;

  private List<PaymentInfoSpecification> specifications;

  /**
   * Method which filter payments based on PaymentInfoRequest.
   *
   * @param paymentInfoRequest {@link PaymentInfoRequest}
   * @param cazId for payment
   * @return  list of {@link PaymentInfo}
   */
  public List<PaymentInfo> filter(PaymentInfoRequest paymentInfoRequest, UUID cazId) {
    Specification<PaymentInfo> specification = specifications.stream()
        .filter(paymentInfoSpecification -> paymentInfoSpecification.shouldUse(paymentInfoRequest))
        .map(paymentInfoSpecification -> paymentInfoSpecification.create(paymentInfoRequest))
        .reduce(initSpecification(cazId), Specification::and);

    return paymentInfoRepository.findAll(specification);
  }

  /**
   * Method to init specification.
   *
   * @param cazId for payment
   * @return {@link Specification}
   */
  private Specification<PaymentInfo> initSpecification(UUID cazId) {
    return new CazIdSpecification(cazId);
  }

}
