package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChargeableAccountVehicleResponse {
  
  PaidPaymentsResponse chargeableAccountVehicles;
  String firstVrn;
  String lastVrn;

}
