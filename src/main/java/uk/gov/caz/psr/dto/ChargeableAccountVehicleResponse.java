package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChargeableAccountVehicleResponse {
  
  ChargeableAccountVehiclesResult chargeableAccountVehicles;
  String firstVrn;
  String lastVrn;

}
