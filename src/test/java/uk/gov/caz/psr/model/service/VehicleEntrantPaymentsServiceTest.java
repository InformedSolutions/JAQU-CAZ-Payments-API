package uk.gov.caz.psr.model.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

public class VehicleEntrantPaymentsServiceTest {

  VehicleEntrantPaymentsService vehicleEntrantPaymentsService;

  @BeforeEach
  void init() {
    vehicleEntrantPaymentsService = new VehicleEntrantPaymentsService();
  }

  @Test
  void canGetCleanAirZoneIDFromVehicleEntrantPaymentList() {

    UUID cleanAirZoneId = UUID.randomUUID();
    LocalDate travelDate = LocalDate.now();
    InternalPaymentStatus internalPaymentStatus = InternalPaymentStatus.NOT_PAID;

    VehicleEntrantPayment vep1 = VehicleEntrantPayment.builder().chargePaid(8000)
        .cleanZoneId(cleanAirZoneId).internalPaymentStatus(internalPaymentStatus)
        .travelDate(travelDate).vrn("testVrn").build();
    VehicleEntrantPayment vep2 = VehicleEntrantPayment.builder().chargePaid(8000)
        .cleanZoneId(cleanAirZoneId).internalPaymentStatus(internalPaymentStatus)
        .travelDate(travelDate).vrn("testVrn").build();

    ArrayList<VehicleEntrantPayment> vehicleEntrantPayments =
        new ArrayList<VehicleEntrantPayment>();
    vehicleEntrantPayments.add(vep1);
    vehicleEntrantPayments.add(vep2);

    Optional<UUID> cazId = vehicleEntrantPaymentsService.findCazId(vehicleEntrantPayments);
    assertTrue(cazId.isPresent());
    assertEquals(cleanAirZoneId, cazId.get());
  }

  @Test
  void getEmptyOptionalIfEmptyVehicleEntrantPayments() {
    Optional<UUID> cazId =
        vehicleEntrantPaymentsService.findCazId(new ArrayList<VehicleEntrantPayment>());
    assertTrue(!cazId.isPresent());
  }

}
