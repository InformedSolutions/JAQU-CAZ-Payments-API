package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;

/**
 * A utility class that maps instances of {@link PaymentInfoRequest} to {@link
 * PaymentInfoRequestAttributes}.
 */
@Component
public class PaymentInfoRequestConverter {

  /**
   * Converts the passed {@code request} to an instance of {@link PaymentInfoRequestAttributes}.
   *
   * @param request A request that is to be converted.
   * @return An instance of {@link PaymentInfoRequestAttributes} with attributes mapped from {@code
   *     request}.
   */
  public PaymentInfoRequestAttributes toPaymentInfoRequestAttributes(PaymentInfoRequest request) {
    return PaymentInfoRequestAttributes.builder()
        .externalPaymentId(request.getPaymentProviderId())
        .fromDatePaidFor(request.getFromDatePaidFor())
        .toDatePaidFor(request.getToDatePaidFor())
        .vrn(AttributesNormaliser.normalizeVrn(request.getVrn()))
        .build();
  }
}
