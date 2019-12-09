package uk.gov.caz.psr.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.controller.exception.PaymentInfoDtoValidationException;
import uk.gov.caz.psr.controller.exception.PaymentStatusDtoValidationException;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentStatusRequest;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.dto.PaymentUpdateSuccessResponse;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.service.ChargeSettlementPaymentInfoService;
import uk.gov.caz.psr.service.ChargeSettlementService;
import uk.gov.caz.psr.service.PaymentStatusUpdateService;
import uk.gov.caz.psr.util.VehicleEntrantPaymentInfoConverter;

/**
 * A controller which exposes endpoints dealing with charge settlement.
 */
@RestController
@AllArgsConstructor
public class ChargeSettlementController implements ChargeSettlementControllerApiSpec {

  public static final String BASE_PATH = "/v1/charge-settlement";
  public static final String PAYMENT_INFO_PATH = "/payment-info";
  public static final String PAYMENT_STATUS_PATH = "/payment-status";

  private final PaymentStatusUpdateService paymentStatusUpdateService;
  private final ChargeSettlementService chargeSettlementService;
  private final ChargeSettlementPaymentInfoService chargeSettlementPaymentInfoService;
  private final VehicleEntrantPaymentInfoConverter vehicleEntrantPaymentInfoConverter;

  @Override
  public ResponseEntity<PaymentInfoResponse> getPaymentInfo(PaymentInfoRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentInfoDtoValidationException("paymentInfo.validationErrorTitle",
          bindingResult);
    }

    List<VehicleEntrantPaymentInfo> filter = chargeSettlementPaymentInfoService
        .findPaymentInfo(request, cleanAirZoneId);
    return ResponseEntity.ok(vehicleEntrantPaymentInfoConverter.toPaymentInfoResponse(filter));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus(PaymentStatusRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentStatusDtoValidationException(request.getVrn(),
          "getPaymentStatus.validationErrorTitle", bindingResult);
    }

    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(
            cleanAirZoneId,
            request.getVrn(),
            LocalDate.parse(request.getDateOfCazEntry()));

    return ResponseEntity.ok(PaymentStatusResponse.from(paymentStatus));
  }

  @Override
  public PaymentUpdateSuccessResponse updatePaymentStatus(PaymentStatusUpdateRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentStatusDtoValidationException(request.getVrn(),
          "paymentStatusUpdate.validationErrorTitle", bindingResult);
    }
    paymentStatusUpdateService.processUpdate(request.toVehicleEntrantPaymentStatusUpdates(
        cleanAirZoneId));
    return new PaymentUpdateSuccessResponse();
  }
}
