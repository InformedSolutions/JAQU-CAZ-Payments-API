package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntrantRepository;

/**
 * Class responsible for connecting a successful payment with a vehicle entrant record if exists.
 */
@Service
@AllArgsConstructor
@Slf4j
public class FinalizePaymentService {

  private final VehicleEntrantRepository vehicleEntrantRepository;

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
    if (payment.getExternalPaymentStatus() != ExternalPaymentStatus.SUCCESS) {
      log.info("Payment status is not equal to '{}', but '{}', hence not trying to connect it "
              + "to an existing vehicle entrant", ExternalPaymentStatus.SUCCESS,
          payment.getExternalPaymentStatus());
      return payment;
    }

    return rebuildPayment(payment);
  }

  /**
   * Rebuilds {@link Payment} to load {@code VehicleEntrantId} in each {@link VehicleEntrantPayment}
   * which belongs to provided {@link Payment}.
   *
   * @param payment provided {@link Payment} object
   */
  private Payment rebuildPayment(Payment payment) {
    return payment.toBuilder()
        .vehicleEntrantPayments(rebuildVehicleEntrantPayments(payment.getVehicleEntrantPayments()))
        .build();
  }

  /**
   * Rebuilds List of {@link VehicleEntrantPayment} to load {@code VehicleEntrantId} in each {@link
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
   * Loads instance of {@link VehicleEntrant} from the database and returns {@code id} if present.
   *
   * @param vehicleEntrantPayment single vehicleEntrantPayment object.
   */
  private UUID loadVehicleEntrantId(VehicleEntrantPayment vehicleEntrantPayment) {
    return vehicleEntrantRepository.findBy(vehicleEntrantPayment.getTravelDate(),
        vehicleEntrantPayment.getCleanZoneId(), vehicleEntrantPayment.getVrn())
        .map(VehicleEntrant::getId)
        .orElse(null);
  }
}
