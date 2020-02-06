package uk.gov.caz.psr.service;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.controller.exception.PaymentInfoVrnValidationException;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.jpa.EntrantPaymentMatchInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.CazIdSpecification;
import uk.gov.caz.psr.service.paymentinfo.LatestPaymentInfoSpecification;
import uk.gov.caz.psr.service.paymentinfo.OmitNotPaidPaymentInfoSpecification;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoSpecification;
import uk.gov.caz.psr.util.AttributesNormaliser;

/**
 * Service which merge specification and run query.
 */
@Service
@AllArgsConstructor
public class ChargeSettlementPaymentInfoService {

  private static final OmitNotPaidPaymentInfoSpecification OMIT_NOT_PAID_PAYMENT_INFO_SPECIFICATION
      = new OmitNotPaidPaymentInfoSpecification();
  private static final LatestPaymentInfoSpecification LATEST_PAYMENT_INFO_SPECIFICATION =
      new LatestPaymentInfoSpecification();

  private final EntrantPaymentMatchInfoRepository entrantPaymentMatchInfoRepository;
  private final EntrantPaymentRepository entrantPaymentRepository;
  private final List<PaymentInfoSpecification> specifications;

  /**
   * Finds payment information based on {@code attributes} and {@code cazId}.
   *
   * @param attributes {@link PaymentInfoRequestAttributes}
   * @param cazId for payment
   * @return A list of {@link EntrantPaymentMatchInfo}
   */
  public List<EntrantPaymentMatchInfo> findPaymentInfo(PaymentInfoRequestAttributes attributes,
      UUID cazId) {
    throwIfNonExistentVrn(cazId, attributes.getVrn());
    Specification<EntrantPaymentMatchInfo> specification = specifications.stream()
        .filter(paymentInfoSpecification -> paymentInfoSpecification.shouldUse(attributes))
        .map(paymentInfoSpecification -> paymentInfoSpecification.create(attributes))
        .reduce(initialSpecification(cazId), Specification::and);

    return entrantPaymentMatchInfoRepository.findAll(specification);
  }

  /**
   * If there is no record of a vrn entering or having paid in a Clean Air Zone 
   * then throw an error.
   * @param cleanAirZoneId the identifier of the Clean Air Zone
   * @param vrn the vrn of the vehicle to check
   */
  private void throwIfNonExistentVrn(UUID cleanAirZoneId, String vrn) {
    if (vrn != null && entrantPaymentRepository.countByVrnAndCaz(cleanAirZoneId, 
          AttributesNormaliser.normalizeVrn(vrn)) == 0) {
      throw new PaymentInfoVrnValidationException("vrn cannot be found");
    }
  }

  /**
   * Returns the initial specification that selects entries for a given {@code cazId} and
   * does not include payments with {@code notPaid} status.
   */
  private Specification<EntrantPaymentMatchInfo> initialSpecification(UUID cazId) {
    return CazIdSpecification.forCaz(cazId)
        .and(LATEST_PAYMENT_INFO_SPECIFICATION)
        .and(OMIT_NOT_PAID_PAYMENT_INFO_SPECIFICATION);
  }
}
