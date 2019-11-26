package uk.gov.caz.psr.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.PaymentRepository2;
import uk.gov.caz.psr.service.paymentinfo.PaymentSpecifications;

/**
 * sdfsadfsadf dsf.
 */
@AllArgsConstructor
public class PaymentInfoService {

  private List<PaymentInfoRepository> paymentInfoRepositories;

  private PaymentRepository2 paymentRepository2;

  private PaymentSpecifications paymentSpecifications;
  /**
   *  dsfsdfsdf.
   *
   * @param paymentInfoRequest sdfsdf
   * @return sdfsdf
   */
  public Payment findPaymentInfo(PaymentInfoRequest paymentInfoRequest) {
    return paymentInfoRepositories.stream()
        .filter(paymentInfoRepository -> paymentInfoRepository.shouldUse(paymentInfoRequest))
        .map(paymentInfoRepository -> {
          paymentInfoRepository.validate(paymentInfoRequest);
          return paymentInfoRepository.find(paymentInfoRequest)
              .orElseThrow(() -> new IllegalArgumentException("Payment info not found"));
        }).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Payment info not found"));
  }

  public List<Payment> findPaymentInfo2(PaymentInfoRequest paymentInfoRequest) {
   return paymentRepository2.findAll(paymentSpecifications.specification(paymentInfoRequest));
  }
}
