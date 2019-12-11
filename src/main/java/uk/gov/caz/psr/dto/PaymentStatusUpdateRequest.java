package uk.gov.caz.psr.dto;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrantPaymentStatusUpdate;

/**
 * A value object which is used as a request for updating payment status.
 */
@Value
@Builder
public class PaymentStatusUpdateRequest {

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.vrn}")
  @NotBlank
  @Size(min = 1, max = 15)
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.payment-status-update.status-updates}")
  @NotEmpty
  @Valid
  List<PaymentStatusUpdateDetails> statusUpdates;

  /**
   * Maps this value object to list of the models.
   *
   * @param cleanAirZoneId Id of Clean Air Zone.
   * @return An list of {@link VehicleEntrantPaymentStatusUpdate} whose parameters comes from this
   *     object.
   */
  public List<VehicleEntrantPaymentStatusUpdate> toVehicleEntrantPaymentStatusUpdates(
      UUID cleanAirZoneId) {
    return statusUpdates.stream()
        .map(singlePaymentDetails -> prepareVehicleEntrantPaymentStatusUpdate(cleanAirZoneId,
            singlePaymentDetails))
        .collect(Collectors.toList());
  }

  private VehicleEntrantPaymentStatusUpdate prepareVehicleEntrantPaymentStatusUpdate(
      UUID cleanAirZoneId, PaymentStatusUpdateDetails paymentStatusUpdateDetail) {
    return VehicleEntrantPaymentStatusUpdate.builder()
        .cleanAirZoneId(cleanAirZoneId)
        .vrn(normalizeVrn(vrn))
        .dateOfCazEntry(paymentStatusUpdateDetail.getDateOfCazEntry())
        .paymentStatus(InternalPaymentStatus
            .valueOf(paymentStatusUpdateDetail.getChargeSettlementPaymentStatus().name()))
        .externalPaymentId(paymentStatusUpdateDetail.getPaymentProviderId())
        .caseReference(paymentStatusUpdateDetail.getCaseReference())
        .build();
  }
}
