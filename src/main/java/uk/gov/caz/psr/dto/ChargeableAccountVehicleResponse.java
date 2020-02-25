package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChargeableAccountVehicleResponse {
  
  List<String> vrns;
  String firstVrn;
  String lastVrn;

}
