package uk.gov.caz.psr.util;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.ChargeableVehiclesPage;

/**
 * Model-to-dto-converter for a list of {@link ChargeableVehicle}.
 */
@Component
public class ChargeableVehiclesToDtoConverter {

  /**
   * Converts the passed variables to an instance of {@link ChargeableAccountVehicleResponse}.
   */
  public ChargeableAccountVehicleResponse toChargeableAccountVehicleResponse(
      ChargeableVehiclesPage chargeableVehiclesPage) {

    return ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(
            chargeableAccountVehiclesResultFrom(chargeableVehiclesPage.getChargeableVehicles()))
        .totalVehiclesCount(chargeableVehiclesPage.getTotalVehiclesCount())
        .pageCount(chargeableVehiclesPage.getPageCount())
        .build();
  }

  /**
   * Converts chargeableVehicles list to {@link ChargeableAccountVehiclesResult}.
   */
  private ChargeableAccountVehiclesResult chargeableAccountVehiclesResultFrom(
      List<ChargeableVehicle> chargeableVehicles) {
    return ChargeableAccountVehiclesResult.builder()
        .results(chargeableVehicles.stream()
            .map(chargeableVehicle -> VrnWithTariffAndEntrancesPaid.builder()
                .vrn(chargeableVehicle.getVrn())
                .charge(chargeableVehicle.getCharge())
                .tariffCode(chargeableVehicle.getTariffCode())
                .paidDates(chargeableVehicle.getPaidDates())
                .build())
            .collect(Collectors.toList()))
        .build();
  }
}
