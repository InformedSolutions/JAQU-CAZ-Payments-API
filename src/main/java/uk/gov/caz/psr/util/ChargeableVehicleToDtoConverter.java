package uk.gov.caz.psr.util;

import java.util.Collections;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.model.ChargeableVehicle;

/**
 * Model-to-dto-converter for a single {@link ChargeableVehicle}.
 */
@Component
public class ChargeableVehicleToDtoConverter {

  /**
   * Converts the passed variables to an instance of {@link ChargeableAccountVehicleResponse}.
   */
  public ChargeableAccountVehicleResponse toChargeableAccountVehicleResponse(
      ChargeableVehicle chargeableVehicle) {

    return ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(chargeableAccountVehiclesResultFrom(chargeableVehicle))
        .build();
  }

  private ChargeableAccountVehiclesResult chargeableAccountVehiclesResultFrom(
      ChargeableVehicle chargeableVehicle) {
    return ChargeableAccountVehiclesResult.builder()
        .results(Collections.singletonList(VrnWithTariffAndEntrancesPaid.builder()
            .vrn(chargeableVehicle.getVrn())
            .charge(chargeableVehicle.getCharge())
            .tariffCode(chargeableVehicle.getTariffCode())
            .paidDates(chargeableVehicle.getPaidDates())
            .build()))
        .build();
  }
}
