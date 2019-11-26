package uk.gov.caz.psr.service.paymentinfo;

import java.util.Optional;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.Payment;

public interface PaymentInfoRepository {

  boolean shouldUse(PaymentInfoRequest paymentInfoRequest);

  Optional<Payment> find(PaymentInfoRequest paymentInfoRequest);

  void validate(PaymentInfoRequest paymentInfoRequest);

}
