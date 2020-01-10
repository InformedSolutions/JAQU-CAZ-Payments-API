package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;

/**
 * Creates a new instance of {@link Payment} with the new external status set and a mapped internal
 * one in associated vehicle entrant payments.
 */
@Service
public class PaymentWithExternalPaymentDetailsBuilder {

  /**
   * Creates a new instance of {@link Payment} based on the passed {@code payment} with {@code
   * newStatus} set and mapped internal one in associated vehicle entrant payments.
   */
  public Payment buildPaymentWithExternalPaymentDetails(Payment payment,
      ExternalPaymentDetails externalPaymentDetails) {
    checkPreconditions(payment, externalPaymentDetails);
    ExternalPaymentStatus newStatus = externalPaymentDetails.getExternalPaymentStatus();

    return payment.toBuilder()
        .externalPaymentStatus(newStatus)
        .emailAddress(externalPaymentDetails.getEmail())
        .authorisedTimestamp(getAuthorisedTimestamp(payment, newStatus))
        .build();
    // TODO: Fix with the payment updates CAZ-1716
    //     .vehicleEntrantPayments(buildVehicleEntrantPaymentsWith(newStatus,
    //            payment.getVehicleEntrantPayments())
    //        )
  }

  /**
   * Verifies whether passed {@code payment} and {@status} are in valid state when calling {@link
   * PaymentWithExternalPaymentDetailsBuilder#buildPaymentWithExternalPaymentDetails(
   *uk.gov.caz.psr.model.Payment, uk.gov.caz.psr.model.ExternalPaymentDetails)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentDetails externalPaymentDetails) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(externalPaymentDetails, "externalPaymentDetails cannot be null");
    Preconditions.checkArgument(
        externalPaymentDetails.getExternalPaymentStatus() != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        externalPaymentDetails.getExternalPaymentStatus(), payment.getExternalPaymentStatus());
  }

  //  /**
  //   * Creates a new list of {@link VehicleEntrantPayment} with an internal status mapped from
  //   * {@code status}.
  //   */
  //  private List<VehicleEntrantPayment> buildVehicleEntrantPaymentsWith(
  //      ExternalPaymentStatus status,
  //      List<VehicleEntrantPayment> vehicleEntrantPayments) {
  //    return vehicleEntrantPayments
  //        .stream()
  //        .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
  //            .internalPaymentStatus(InternalPaymentStatus.from(status))
  //            .build())
  //        .collect(Collectors.toList());
  //  }

  /**
   * Returns {@link LocalDateTime#now()} as the authorised payment timestamp (date and time when the
   * payment has been successfully processed by GOV UK Pay) provided {@code newExternalStatus} is
   * equal to {@link ExternalPaymentStatus#SUCCESS}. Otherwise, the current value is obtained from
   * {@code payment}.
   */
  private LocalDateTime getAuthorisedTimestamp(Payment payment,
      ExternalPaymentStatus newExternalStatus) {
    if (newExternalStatus == ExternalPaymentStatus.SUCCESS) {
      return LocalDateTime.now();
    }
    return payment.getAuthorisedTimestamp();
  }
}
