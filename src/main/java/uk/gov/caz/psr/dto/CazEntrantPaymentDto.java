package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.CazEntrantPayment;

/**
 * A value object that stores data after creation/update of CAZ_ENTRANT_PAYMENT table.
 */
@Value
@Builder
public class CazEntrantPaymentDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.vehicle-entrant-id")
  @NotNull
  UUID cleanAirZoneEntrantPaymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.status}")
  @NotNull
  String paymentStatus;

  /**
   * Maps {@link CazEntrantPayment} to {@link CazEntrantPaymentDto}.
   * @param cazEntrantPayment record to be mapped
   * @return mapped object
   */
  public static CazEntrantPaymentDto from(CazEntrantPayment cazEntrantPayment) {
    return CazEntrantPaymentDto
        .builder()
        .cleanAirZoneEntrantPaymentId(cazEntrantPayment.getCleanAirZoneEntrantPaymentId())
        .paymentStatus(cazEntrantPayment.getInternalPaymentStatus().toString())
        .build();
  }
}
