package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * An entity which represents a row stored in the database in {@code VEHICLE_ENTRANCE} table.
 */
@Data
@Builder
public class VehicleEntrance {
  UUID id;
  private final UUID cleanZoneId;
  private final LocalDateTime cazEntryTimestamp;
  private final LocalDate cazEntryDate;
  private final String vrn;
}
