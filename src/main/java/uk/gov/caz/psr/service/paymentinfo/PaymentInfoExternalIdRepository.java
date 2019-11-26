package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * sdfsafd  ef .
 */
@AllArgsConstructor
@Order(1)
public class PaymentInfoExternalIdRepository implements PaymentInfoRepository {

  private PaymentRepository paymentRepository;

  @Override
  public boolean shouldUse(PaymentInfoRequest paymentInfoRequest) {
    return Optional.ofNullable(paymentInfoRequest.getPaymentId()).isPresent();
  }

  @Override
  public Optional<Payment> find(PaymentInfoRequest paymentInfoRequest) {
    return paymentRepository.findByExternalId(paymentInfoRequest.getPaymentId());
  }

  @Override
  public void validate(PaymentInfoRequest paymentInfoRequest) {
    if (paymentInfoRequest.getVrn() != null
        || paymentInfoRequest.getFromDatePaidFor() != null
        || paymentInfoRequest.getToDatePaidFor() != null) {
      throw new IllegalArgumentException("Only paymentId should exists on request");
    }


  }
}
