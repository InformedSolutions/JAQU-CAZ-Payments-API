package uk.gov.caz.psr.dto.directdebit;

import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;

@Value
@Builder
public class CreateDirectDebitPaymentResponse {

  @NonNull
  UUID paymentId;

  @NonNull
  Long referenceNumber;

  @NonNull
  String externalPaymentId;

  @NonNull
  ExternalPaymentStatus paymentStatus;

  /**
   * Helper method to map {@link Payment} to ${@link CreateDirectDebitPaymentResponse}.
   *
   * @param payment {@link Payment}
   * @return {@link CreateDirectDebitPaymentResponse}
   */
  public static CreateDirectDebitPaymentResponse from(Payment payment) {
    return CreateDirectDebitPaymentResponse.builder()
        .externalPaymentId(payment.getExternalId())
        .paymentId(payment.getId())
        .referenceNumber(payment.getReferenceNumber())
        .paymentStatus(payment.getExternalPaymentStatus())
        .build();
  }
}
