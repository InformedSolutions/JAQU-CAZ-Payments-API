package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for vehicle retrieval response for vehicles charges.
 */
@Value
@Builder
public class VehicleDetails {

  /**
   * Vehicle Registration number.
   */
  String vrn;

  /**
   * Flag if vehicle is exempt.
   */
  Boolean isExempt;

  /**
   * Flag if vehicle is Retrofitted.
   */
  Boolean isRetrofitted;

  /**
   * Vehice Type.
   */
  String vehicleType;

  /**
   * List of cached charges for each CAZ.
   */
  List<CachedCharge> cachedCharges;
}