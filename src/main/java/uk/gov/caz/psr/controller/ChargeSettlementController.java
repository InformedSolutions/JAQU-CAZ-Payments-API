package uk.gov.caz.psr.controller;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.service.ChargeSettlementPaymentInfoService;
import uk.gov.caz.psr.service.ChargeSettlementService;
import uk.gov.caz.psr.service.PaymentStatusUpdateService;
import uk.gov.caz.psr.util.EntrantPaymentInfoConverter;
import uk.gov.caz.psr.util.PaymentInfoRequestConverter;

/**
 * A controller which exposes endpoints dealing with charge settlement.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class ChargeSettlementController implements ChargeSettlementControllerApiSpec {

  public static final String TIMESTAMP = "timestamp";
  public static final String BASE_PATH = "/v1/charge-settlement";
  public static final String PAYMENT_INFO_PATH = "/payment-info";
  public static final String PAYMENT_STATUS_PATH = "/payment-status";

  private final PaymentStatusUpdateService paymentStatusUpdateService;
  private final ChargeSettlementService chargeSettlementService;
  private final ChargeSettlementPaymentInfoService chargeSettlementPaymentInfoService;
  private final EntrantPaymentInfoConverter entrantPaymentInfoConverter;
  private final PaymentInfoRequestConverter paymentInfoRequestConverter;

  @Override
  public ResponseEntity<PaymentInfoResponse> getPaymentInfo(PaymentInfoRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentInfoDtoValidationException("paymentInfo.validationErrorTitle",
          bindingResult);
    }
    request.validateParametersConjunction();

    log.info("Got 'get payment info' request: {}", request);

    List<EntrantPaymentMatchInfo> result = chargeSettlementPaymentInfoService.findPaymentInfo(
        paymentInfoRequestConverter.toPaymentInfoRequestAttributes(request),
        cleanAirZoneId
    );
    log.info("Found {} matching vehicle entrant payments for payment-info request {}",
        result.size(), request);
    return ResponseEntity.ok(entrantPaymentInfoConverter.toPaymentInfoResponse(result));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus(PaymentStatusRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentStatusDtoValidationException(request.getVrn(),
          "getPaymentStatus.validationErrorTitle", bindingResult);
    }
    log.info("Got 'get payment status' request: {}", request);

    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(
            cleanAirZoneId,
            normalizeVrn(request.getVrn()),
            LocalDate.parse(request.getDateOfCazEntry()));

    return paymentStatus.map(PaymentStatusResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(PaymentStatusResponse.notFound()));
  }

  @Override
  public PaymentUpdateSuccessResponse updatePaymentStatus(PaymentStatusUpdateRequest request,
      BindingResult bindingResult, UUID cleanAirZoneId, LocalDateTime timestamp) {
    if (bindingResult.hasErrors()) {
      throw new PaymentStatusDtoValidationException(request.getVrn(),
          "paymentStatusUpdate.validationErrorTitle", bindingResult);
    }
    log.info("Got 'update payment status' request: {}", request);

    paymentStatusUpdateService.process(request.toEntrantPaymentStatusUpdates(cleanAirZoneId));
    return new PaymentUpdateSuccessResponse();
  }
}
