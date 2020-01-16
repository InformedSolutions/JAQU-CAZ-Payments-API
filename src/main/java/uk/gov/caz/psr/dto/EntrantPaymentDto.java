package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.EntrantPayment;

/**
 * A value object that stores data after creation/update of T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT table.
 */
@Value
@Builder
public class EntrantPaymentDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.vehicle-entrant-id")
  @NotNull
  UUID cleanAirZoneEntrantPaymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.status}")
  @NotNull
  String paymentStatus;

  /**
   * Maps {@link EntrantPayment} to {@link EntrantPaymentDto}.
   * @param entrantPayment record to be mapped
   * @return mapped object
   */
  public static EntrantPaymentDto from(EntrantPayment entrantPayment) {
    return EntrantPaymentDto
        .builder()
        .cleanAirZoneEntrantPaymentId(entrantPayment.getCleanAirZoneEntrantPaymentId())
        .paymentStatus(entrantPayment.getInternalPaymentStatus().toString())
        .build();
  }
}
