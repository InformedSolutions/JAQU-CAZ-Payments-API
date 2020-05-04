package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

/**
 * Class that represents a vehicle entrant for a given {@code cleanAirZoneId} used in Payment
 * creation.
 */
@Value
@Builder(toBuilder = true)
public class Transaction {
  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.vrn}")
  @ToString.Exclude
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.charge}")
  Integer charge;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.travel-date}")
  LocalDate travelDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.tariff-code}")
  String tariffCode;
}
