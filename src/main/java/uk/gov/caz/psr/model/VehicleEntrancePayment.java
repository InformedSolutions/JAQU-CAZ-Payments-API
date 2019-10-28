package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * An entity which represents a row stored in the database {@code VEHICLE_ENTRANCE_PAYMENT} table.
 */
@Data
@Builder
public class VehicleEntrancePayment {
  UUID id;
  private final UUID paymentId;
  private final UUID vehicleEntranceId;
  private final LocalDate dateOfEntrance;
  private final String vrn;
}
