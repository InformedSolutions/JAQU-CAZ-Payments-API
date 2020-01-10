package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.VehicleEntrantPaymentStatusUpdate;
import uk.gov.caz.psr.repository.CazEntrantPaymentRepository;

/**
 * A service which updates {@code paymentStatus} for all found {@link CazEntrantPayment}.
 */
@Service
@Slf4j
@AllArgsConstructor
public class PaymentStatusUpdateService {

  private final CazEntrantPaymentRepository cazEntrantPaymentRepository;

  /**
   * Process update of the {@link CazEntrantPayment} with provided details.
   *
   * @param vehicleEntrantPaymentStatusUpdates list of {@link VehicleEntrantPaymentStatusUpdate}
   *                                           which contains data to find and update {@link
   *                                           CazEntrantPayment}.
   */
  public void processUpdate(
      List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdates) {
    Preconditions.checkNotNull(vehicleEntrantPaymentStatusUpdates,
        "vehicleEntrantPaymentStatusUpdates cannot be null");
    //  TODO: Fix with the payment updates CAZ-1716
    //  List<VehicleEntrantPayment> vehicleEntrantPayments = prepareVehicleEntrantPayments(
    //      vehicleEntrantPaymentStatusUpdates);
    //  vehicleEntrantPaymentRepository.update(vehicleEntrantPayments);
  }

  //  /**
  //   * Builds list of {@link VehicleEntrantPayment} to be updated.
  //   *
  //   * @param vehicleEntrantPaymentStatusUpdates list of {@link VehicleEntrantPaymentStatusUpdate}
  //   *                                           which contains data to find and update {@link
  //   *                                           VehicleEntrantPayment}.
  //   */
  //  private List<VehicleEntrantPayment> prepareVehicleEntrantPayments(
  //      List<VehicleEntrantPaymentStatusUpdate> vehicleEntrantPaymentStatusUpdates) {
  //
  //    return vehicleEntrantPaymentStatusUpdates.stream()
  //        .map(this::prepareVehicleEntrantPayment)
  //        .collect(Collectors.toList());
  //  }
  //
  //  /**
  //   * Builds {@link VehicleEntrantPayment} with updated status.
  //   *
  //   * @param vehicleEntrantPaymentStatusUpdate {@link VehicleEntrantPaymentStatusUpdate} which
  //   *                                          contains data to find and update {@link
  //   *                                          VehicleEntrantPayment}.
  //   */
  //  private VehicleEntrantPayment prepareVehicleEntrantPayment(
  //      VehicleEntrantPaymentStatusUpdate vehicleEntrantPaymentStatusUpdate) {
  //    VehicleEntrantPayment vehicleEntrantPayment = loadVehicleEntrantPayment(
  //        vehicleEntrantPaymentStatusUpdate).orElseThrow(
  //          () -> new MissingVehicleEntrantPaymentException(
  //              vehicleEntrantPaymentStatusUpdate.getVrn(),
  //              "VehicleEntrantPayment not found for: " + vehicleEntrantPaymentStatusUpdate));
  //
  //    return vehicleEntrantPayment.toBuilder()
  //        .internalPaymentStatus(vehicleEntrantPaymentStatusUpdate.getPaymentStatus())
  //        .caseReference(vehicleEntrantPaymentStatusUpdate.getCaseReference())
  //        .build();
  //  }
  //
  //  /**
  //   * Loads {@link VehicleEntrantPayment} from the repository for the provided details.
  //   *
  //   * @param vehicleEntrantPaymentStatusUpdate {@link VehicleEntrantPaymentStatusUpdate} which
  //   *                                          contains data to find and update {@link
  //   *                                          VehicleEntrantPayment}.
  //   */
  //  private Optional<VehicleEntrantPayment> loadVehicleEntrantPayment(
  //      VehicleEntrantPaymentStatusUpdate vehicleEntrantPaymentStatusUpdate) {
  //
  //    if (Strings.isNullOrEmpty(vehicleEntrantPaymentStatusUpdate.getExternalPaymentId())) {
  //      return vehicleEntrantPaymentRepository
  //          .findOnePaidByVrnAndCazEntryDate(
  //              vehicleEntrantPaymentStatusUpdate.getCleanAirZoneId(),
  //              vehicleEntrantPaymentStatusUpdate.getVrn(),
  //              vehicleEntrantPaymentStatusUpdate.getDateOfCazEntry());
  //    }
  //    return vehicleEntrantPaymentRepository
  //        .findOnePaidByCazEntryDateAndExternalPaymentId(
  //            vehicleEntrantPaymentStatusUpdate.getCleanAirZoneId(),
  //            vehicleEntrantPaymentStatusUpdate.getDateOfCazEntry(),
  //            vehicleEntrantPaymentStatusUpdate.getExternalPaymentId());
  //
  //  }
}
