package uk.gov.caz.psr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in {@code VEHICLE_ENTRANT} table.
 */
@Value
@Builder(toBuilder = true)
public class VehicleEntrant {

  /**
   * A unique database identifier.
   */
  UUID id;

  /**
   * A unique identifier for the Clean Air Zone.
   */
  @NonNull
  UUID cleanAirZoneId;

  /**
   * Date with time when a vehicle entered the CAZ for the first time on the given date.
   */
  @NonNull
  LocalDateTime cazEntryTimestamp;

  /**
   * A date when a vehicle entered the CAZ.
   */
  @NonNull
  LocalDate cazEntryDate;

  /**
   * Vehicle registration number.
   */
  @NonNull
  String vrn;
}
