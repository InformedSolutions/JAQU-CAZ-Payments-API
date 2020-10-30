package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the returned JSON when client asks for account vehicles
 * that are chargeable.
 */
@Value
@Builder
public class ChargeableAccountVehiclesResult {

  /**
   * A list of vrns with associated charge, tariff code and paid travel dates.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.chargeable-account-vehicles-result.results}")
  List<VrnWithTariffAndEntrancesPaid> results;

  @Value
  @Builder(toBuilder = true)
  public static class VrnWithTariffAndEntrancesPaid {

    /**
     * A VRN for which the list of paid days is being returned.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.vrn}")
    String vrn;

    /**
     * The tariff code for the vehicle in a given Clean Air Zone.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.vrn-with-tariff-entrances.tariff-code}")
    String tariffCode;

    /**
     * The charge incurred by the vehicle in a given Clean Air Zone.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.charge}")
    BigDecimal charge;

    /**
     * A list of days which are already paid.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.paid-dates}")
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    List<LocalDate> paidDates;
  }

}
