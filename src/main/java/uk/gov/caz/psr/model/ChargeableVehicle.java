package uk.gov.caz.psr.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;

/**
 * A class which holds attributes related for chargeable vehicle.
 */
@Value
@Builder(toBuilder = true)
public class ChargeableVehicle {

  /**
   * A VRN for which the list of paid days is being returned.
   */
  String vrn;

  /**
   * The tariff code for the vehicle in a given Clean Air Zone.
   */
  String tariffCode;

  /**
   * The charge incurred by the vehicle in a given Clean Air Zone.
   */
  BigDecimal charge;

  /**
   * A list of days which are already paid.
   */
  List<LocalDate> paidDates;

  /**
   * Method to build {@link ChargeableVehicle} object based on cached charge.
   */
  public static ChargeableVehicle from(String vrn, VehicleCharge cachedCharge) {
    return ChargeableVehicle.builder()
        .vrn(vrn)
        .charge(cachedCharge.getCharge())
        .tariffCode(cachedCharge.getTariffCode())
        .build();
  }

  /**
   * Method to build {@link ChargeableVehicle} object based on cached charge and paid dates.
   */
  public static ChargeableVehicle from(String vrn, VehicleCharge cachedCharge,
      List<LocalDate> paidDates) {
    return ChargeableVehicle.builder()
        .vrn(vrn)
        .charge(cachedCharge.getCharge())
        .tariffCode(cachedCharge.getTariffCode())
        .paidDates(paidDates)
        .build();
  }
}
