// TODO: We are not longer supporting adding VehicleEntrant will keep it here
//       until the process is fixed.
//package uk.gov.caz.psr.service;
//
//import com.google.common.base.Preconditions;
//import java.time.LocalDate;
//import java.util.Optional;
//import java.util.UUID;
//import lombok.AllArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import uk.gov.caz.psr.model.InternalPaymentStatus;
//import uk.gov.caz.psr.model.VehicleEntrant;
//import uk.gov.caz.psr.model.VehicleEntrantPayment;
//import uk.gov.caz.psr.repository.VehicleEntrantRepository;
//
///**
// * A service which is responsible for keeping the database synchronized with the entrant and
// * payments data.
// */
//@Service
//@AllArgsConstructor
//public class VehicleEntrantService {
//
//  private final VehicleEntrantRepository vehicleEntrantRepository;
//  private final FinalizeVehicleEntrantService finalizeVehicleEntrantService;
//
//  /**
//   * Inserts {@code vehicleEntrant} into database (unless it exists) and connects it with the
//   * payment information provided it is valid.
//   *
//   * @param vehicleEntrant A record which is to be inserted into the database.
//   * @throws NullPointerException if {@code vehicleEntrant} is {@code null}
//   * @throws IllegalArgumentException if {@code vehicleEntrant#id} is {@code null}
//   * @return InternalPaymentStatus for created Vehicle Entrant
//   */
//  @Transactional
//  public InternalPaymentStatus registerVehicleEntrant(VehicleEntrant vehicleEntrant) {
//    Preconditions.checkNotNull(vehicleEntrant, "Vehicle entrant cannot be null");
//    Preconditions.checkArgument(vehicleEntrant.getId() == null,
//        "ID of the vehicle entrant must be null, actual: %s", vehicleEntrant.getId());
//
//    VehicleEntrant existingVehicleEntrant = insertOrFindExisting(vehicleEntrant);
//    Optional<VehicleEntrantPayment> vehicleEntrantPayment = finalizeVehicleEntrantService
//        .connectExistingVehicleEntrantPayment(existingVehicleEntrant);
//    return vehicleEntrantPayment
//        .map(VehicleEntrantPayment::getInternalPaymentStatus)
//        .orElse(InternalPaymentStatus.NOT_PAID);
//  }
//
//  /**
//   * Inserts {@code vehicleEntrant} into to the database or, if the entity exists, finds it. The
//   * entity is returned in both cases.
//   *
//   * @throws IllegalStateException if both insert and find operations fail.
//   */
//  private VehicleEntrant insertOrFindExisting(VehicleEntrant vehicleEntrant) {
//    return vehicleEntrantRepository.insertIfNotExists(vehicleEntrant)
//        .orElseGet(() -> {
//              LocalDate cazEntryDate = vehicleEntrant.getCazEntryDate();
//              UUID cleanZoneId = vehicleEntrant.getCleanZoneId();
//              String vrn = vehicleEntrant.getVrn();
//
//              return vehicleEntrantRepository.findBy(
//                  cazEntryDate,
//                  cleanZoneId,
//                  vrn
//              ).orElseThrow(() ->
//                  new IllegalStateException(String.format("Cannot find the existing vehicle "
//                      + "entrant (%s, %s, %s)", cazEntryDate, cleanZoneId, vrn)));
//            }
//        );
//  }
//}
