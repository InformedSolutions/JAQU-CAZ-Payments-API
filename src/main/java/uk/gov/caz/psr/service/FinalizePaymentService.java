package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrance;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntranceRepository;

@Service
@AllArgsConstructor
public class FinalizePaymentService {

  private final VehicleEntranceRepository vehicleEntranceRepository;

  /**
   * Adds {@code VehicleEntrantId} to each {@link VehicleEntrantPayment} if {@code Payment} status
   * is SUCCESS.
   *
   * @param payment provided {@link Payment} object
   * @return {@link Payment} with added VehicleEntrantId if Payment status is SUCCESS
   * @throws NullPointerException if {@code payment} is null
   */
  public Payment connectExistingVehicleEntrants(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    if (payment.getStatus() != PaymentStatus.SUCCESS) {
      return payment;
    }

    return rebuildPayment(payment);
  }

  /**
   * Rebuilds {@link Payment} to load {@code VehicleEntranceId} in each {@link
   * VehicleEntrantPayment} which belongs to provided {@link Payment}.
   *
   * @param payment provided {@link Payment} object
   */
  private Payment rebuildPayment(Payment payment) {
    return payment.toBuilder()
        .vehicleEntrantPayments(rebuildVehicleEntrantPayments(payment.getVehicleEntrantPayments()))
        .build();
  }

  /**
   * Rebuilds List of {@link VehicleEntrantPayment} to load {@code VehicleEntranceId} in each {@link
   * VehicleEntrantPayment}.
   *
   * @param vehicleEntrantPayments provided list of {@link VehicleEntrantPayment}
   */
  private List<VehicleEntrantPayment> rebuildVehicleEntrantPayments(
      List<VehicleEntrantPayment> vehicleEntrantPayments) {
    return vehicleEntrantPayments.stream()
        .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
            .vehicleEntrantId(loadVehicleEntrantId(vehicleEntrantPayment))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Loads instance of {@link VehicleEntrance} from the database and returns {@code id} if present.
   *
   * @param vehicleEntrantPayment single vehicleEntrantPayment object.
   */
  private UUID loadVehicleEntrantId(
      VehicleEntrantPayment vehicleEntrantPayment) {
    return vehicleEntranceRepository
        .findBy(vehicleEntrantPayment.getTravelDate(), vehicleEntrantPayment.getCleanZoneId(),
            vehicleEntrantPayment.getVrn())
        .map(VehicleEntrance::getId)
        .orElse(null);
  }
}
