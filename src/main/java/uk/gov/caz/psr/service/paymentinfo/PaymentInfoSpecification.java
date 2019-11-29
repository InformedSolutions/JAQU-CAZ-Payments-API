package uk.gov.caz.psr.service.paymentinfo;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.PaymentInfo;

public interface PaymentInfoSpecification {

  boolean shouldUse(PaymentInfoRequest paymentInfoRequest);

  Specification<PaymentInfo> create(PaymentInfoRequest paymentInfoRequest);
}
