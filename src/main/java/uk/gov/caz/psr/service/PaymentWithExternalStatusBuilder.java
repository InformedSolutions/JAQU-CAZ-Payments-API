package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

/**
 * Creates a new instance of {@link Payment} with the new external status set and a mapped internal
 * one in associated vehicle entrant payments.
 */
@Service
public class PaymentWithExternalStatusBuilder {

  /**
   * Creates a new instance of {@link Payment} based on the passed {@code payment} with {@code
   * newStatus} set and mapped internal one in associated vehicle entrant payments.
   */
  public Payment buildPaymentWithStatus(Payment payment, ExternalPaymentStatus newStatus) {
    checkPreconditions(payment, newStatus);

    return payment.toBuilder()
        .externalPaymentStatus(newStatus)
        .authorisedTimestamp(getAuthorisedTimestamp(payment, newStatus))
        .vehicleEntrantPayments(buildVehicleEntrantPaymentsWith(newStatus,
            payment.getVehicleEntrantPayments())
        )
        .build();
  }

  /**
   * Verifies whether passed {@code payment} and {@status} are in valid state when calling {@link
   * PaymentWithExternalStatusBuilder#buildPaymentWithStatus(uk.gov.caz.psr.model.Payment,
   * uk.gov.caz.psr.model.ExternalPaymentStatus)}.
   */
  private void checkPreconditions(Payment payment, ExternalPaymentStatus newStatus) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(newStatus, "newStatus cannot be null");
    Preconditions.checkArgument(newStatus != payment.getExternalPaymentStatus(),
        "Status cannot be equal to the existing status ('%s' != '%s')",
        newStatus, payment.getExternalPaymentStatus());
  }

  /**
   * Creates a new list of {@link VehicleEntrantPayment} with an internal status mapped from {@code
   * status}.
   */
  private List<VehicleEntrantPayment> buildVehicleEntrantPaymentsWith(ExternalPaymentStatus status,
      List<VehicleEntrantPayment> vehicleEntrantPayments) {
    return vehicleEntrantPayments
        .stream()
        .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
            .internalPaymentStatus(InternalPaymentStatus.from(status))
            .build())
        .collect(Collectors.toList());
  }

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
