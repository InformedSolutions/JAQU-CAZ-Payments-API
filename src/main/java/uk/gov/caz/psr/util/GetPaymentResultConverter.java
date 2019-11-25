package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentDetails;

/**
 * Utility class to convert {@link GetPaymentResult} to models.
 */
@Component
public class GetPaymentResultConverter {

  /**
   * Method to convert {@link GetPaymentResult} DTO to {@link ExternalPaymentDetails} model.
   *
   * @param getPaymentResult object to be converted
   * @return {@link ExternalPaymentDetails} object converted from provided PaymentResult
   */
  public ExternalPaymentDetails toExternalPaymentDetails(GetPaymentResult getPaymentResult) {
    return ExternalPaymentDetails.builder()
        .externalPaymentStatus(getPaymentResult.getPaymentStatus())
        .email(getPaymentResult.getEmail())
        .build();
  }
}
