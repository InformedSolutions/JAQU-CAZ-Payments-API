package uk.gov.caz.psr.dto.whitelist;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Value;

/**
 * Response from the whitelist service for a 'GET v1/whitelisting/vehicles/{vrn}' call.
 */
@Value
public class WhitelistedVehicleResponseDto {
  /**
   * Vehicle registration number.
   */
  String vrn;

  /**
   * Category of a vehicle.
   */
  String category;

  /**
   * The reason why the vehicle was updated.
   */
  String reasonUpdated;

  /**
   * Timestamp of the last operation that modified the vehicle.
   */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime updateTimestamp;

  /**
   * User's sub (external identifier).
   */
  UUID uploaderId;

  /**
   * User's email.
   */
  String email;

  /**
   * The manufacturer of this vehicle.
   */
  String manufacturer;
}
