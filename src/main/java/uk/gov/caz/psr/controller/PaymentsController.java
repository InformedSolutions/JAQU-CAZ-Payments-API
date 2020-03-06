package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.InitiatePaymentResponse;
import uk.gov.caz.psr.dto.PaidPaymentsRequest;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.dto.ReconcilePaymentResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.service.GetPaidEntrantPaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.service.ReconcilePaymentStatusService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;
import uk.gov.caz.psr.util.InitiatePaymentRequestToModelConverter;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentsController implements PaymentsControllerApiSpec {

  @VisibleForTesting
  public static final String BASE_PATH = "/v1/payments";

  @VisibleForTesting
  public static final String GET_PAID_VEHICLE_ENTRANTS = "paid";

  @VisibleForTesting
  public static final String GET_CLEAN_AIR_ZONES = "clean-air-zones";

  @VisibleForTesting
  public static final String GET_COMPLIANCE = "vehicles/{vrn}/compliance";

  @VisibleForTesting
  public static final String GET_VEHICLE_DETAILS = "vehicles/{vrn}/details";

  @VisibleForTesting
  public static final String GET_UNRECOGNISED_VEHICLE_COMPLIANCE =
      "vehicles/unrecognised/{type}/compliance";

  private final InitiatePaymentService initiatePaymentService;
  private final ReconcilePaymentStatusService reconcilePaymentStatusService;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;
  private final CleanAirZoneService cleanAirZoneService;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

  @Override
  public ResponseEntity<InitiatePaymentResponse> initiatePayment(
      InitiatePaymentRequest request) {
    request.validate();
    log.info("Received payment request {}", request);
    Payment payment = initiatePaymentService.createPayment(
        InitiatePaymentRequestToModelConverter.toPayment(request),
        InitiatePaymentRequestToModelConverter.toSingleEntrantPayments(request),
        request.getReturnUrl());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(InitiatePaymentResponse.from(payment));
  }

  @Override
  public ResponseEntity<ReconcilePaymentResponse> reconcilePaymentStatus(
      UUID id) {
    Optional<Payment> payment =
        reconcilePaymentStatusService.reconcilePaymentStatus(id);
    return payment.map(ReconcilePaymentResponse::from).map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<PaidPaymentsResponse> getPaidEntrantPayment(
      PaidPaymentsRequest paymentsRequest) {
    paymentsRequest.validate();

    Map<String, List<EntrantPayment>> results = getPaidEntrantPaymentsService
        .getResults(new HashSet<String>(paymentsRequest.getVrns()),
            paymentsRequest.getStartDate(), paymentsRequest.getEndDate(),
            paymentsRequest.getCleanAirZoneId());

    return ResponseEntity.ok(PaidPaymentsResponse.from(results));
  }

  @Override
  public ResponseEntity<CleanAirZonesResponse> getCleanAirZones() {
    Response<CleanAirZonesResponse> result = cleanAirZoneService.fetchAll();
    return ResponseEntity.status(result.code()).body(result.body());
  }

  @Override
  public ResponseEntity<ComplianceResultsDto> getCompliance(String vrn,
      String zones) {
    Response<ComplianceResultsDto> result =
        vehicleComplianceRetrievalService.retrieveVehicleCompliance(vrn, zones);

    return ResponseEntity.status(result.code()).body(result.body());
  }

  @Override
  public ResponseEntity<VehicleDto> getVehicleDetails(String vrn) {
    Response<VehicleDto> result =
        vehicleComplianceRetrievalService.retrieveVehicleDetails(vrn);

    return ResponseEntity.status(result.code()).body(result.body());
  }

  @Override
  public ResponseEntity<VehicleTypeCazChargesDto> getUnrecognisedVehicle(
      String type, String zones) {
    Response<VehicleTypeCazChargesDto> result =
        vehicleComplianceRetrievalService.retrieveUnknownVehicleCompliance(type,
            zones);

    return ResponseEntity.status(result.code()).body(result.body());
  }

}
