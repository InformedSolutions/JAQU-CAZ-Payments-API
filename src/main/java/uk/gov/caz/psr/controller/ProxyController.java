package uk.gov.caz.psr.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CacheableResponseDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.vccs.RegisterDetailsDto;
import uk.gov.caz.psr.dto.whitelist.WhitelistedVehicleResponseDto;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;
import uk.gov.caz.psr.service.WhitelistService;

@RestController
@AllArgsConstructor
@Slf4j
public class ProxyController implements ProxyControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments";
  public static final String GET_WHITELIST_VEHICLE_DETAILS = "whitelisted-vehicles/{vrn}";
  public static final String GET_CLEAN_AIR_ZONES = "clean-air-zones";
  public static final String GET_COMPLIANCE = "vehicles/{vrn}/compliance";
  public static final String POST_BULK_COMPLIANCE = "vehicles/bulk-compliance";
  public static final String GET_VEHICLE_DETAILS = "vehicles/{vrn}/details";
  public static final String GET_UNRECOGNISED_VEHICLE_COMPLIANCE =
      "vehicles/unrecognised/{type}/compliance";
  public static final String GET_REGISTER_DETAILS = "vehicles/{vrn}/register-details";

  private final WhitelistService whitelistService;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;
  private final CleanAirZoneService cleanAirZoneService;

  @Override
  public ResponseEntity<CleanAirZonesDto> getCleanAirZones() {
    CacheableResponseDto<CleanAirZonesDto> result = cleanAirZoneService.fetchAll();
    return ResponseEntity
        .status(result.getCode())
        .body(result.getBody());
  }

  @Override
  public ResponseEntity<ComplianceResultsDto> getCompliance(String vrn,
      String zones) {
    Response<ComplianceResultsDto> result =
        vehicleComplianceRetrievalService.retrieveVehicleCompliance(vrn, zones);

    return ResponseEntity.status(result.code()).body(result.body());
  }

  @Override
  public ResponseEntity<List<ComplianceResultsDto>> bulkCompliance(String zones,
      List<String> vrns) {
    List<ComplianceResultsDto> result = vehicleComplianceRetrievalService
        .retrieveVehicleCompliance(vrns, zones);
    return ResponseEntity.ok(result);
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

  @Override
  public ResponseEntity<WhitelistedVehicleResponseDto> getWhitelistVehicleDetails(String vrn) {
    CacheableResponseDto<WhitelistedVehicleResponseDto> response = whitelistService
        .getWhitelistVehicle(vrn);
    return ResponseEntity.status(response.getCode()).body(response.getBody());
  }

  @Override
  public ResponseEntity<RegisterDetailsDto> getRegisterDetails(String vrn) {
    Response<RegisterDetailsDto> response = vehicleComplianceRetrievalService
        .getRegisterDetails(vrn);
    return ResponseEntity.status(response.code()).body(response.body());
  }
}
