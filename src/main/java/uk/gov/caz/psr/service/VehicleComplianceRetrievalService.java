package uk.gov.caz.psr.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.vccs.RegisterDetailsDto;
import uk.gov.caz.psr.repository.VccsRepository;

/**
 * Class responsible to call vccs for compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleComplianceRetrievalService {

  private final VccsRepository vccsRepository;

  /**
   * Coordinate asynchronous requests to the vehicle checker to retrieve
   * compliance information on a single VRN.
   * 
   * @param vrn the vrn to query.
   * @param zones a list of zones to check compliance for
   * @return a compliance result response body
   */
  public Response<ComplianceResultsDto> retrieveVehicleCompliance(String vrn,
      String zones) {
    try {
      log.debug("Fetching compliance result from VCCS: start");
      return vccsRepository.findComplianceSync(vrn, zones);
    } finally {
      log.debug("Fetching compliance result from VCCS: finish");
    }
  }

  /**
   * Coordinate asynchronous requests to the vehicle checker to retrieve vehicle
   * details for a single VRN.
   * 
   * @param vrn the vrn to query.
   * @return details of a vehicle wrapped in a response object.
   */
  public Response<VehicleDto> retrieveVehicleDetails(String vrn) {
    try {
      log.debug("Fetching vehicle details from VCCS: start");
      return vccsRepository.findVehicleDetailsSync(vrn);
    } finally {
      log.debug("Fetching vehicle details from VCCS: finish");
    }
  }

  /**
   * Coordinate asynchronous requests to the vehicle checker to retrieve
   * compliance of unknown vehicles against a given clean air zone.
   * 
   * @param type the vehicle type against the CAZ framework.
   * @return details of a compliance outcome wrapped in a response object.
   */
  public Response<VehicleTypeCazChargesDto> retrieveUnknownVehicleCompliance(
      String type, String zones) {
    try {
      log.debug("Fetching unknown compliance details from VCCS: start");
      return vccsRepository.findUnknownVehicleComplianceSync(type, zones);
    } finally {
      log.debug("Fetching unknown compliance details from VCCS: finish");
    }
  }

  /**
   * Coordinate asynchronous requests to the vehicle checker to get register details.
   *
   * @param vrn vehicle registration number of a vehicle
   * @return register details outcome wrapped in a response object.
   */
  public Response<RegisterDetailsDto> getRegisterDetails(String vrn) {
    try {
      log.debug("Fetching register details from VCCS: start");
      return vccsRepository.getRegisterDetailsSync(vrn);
    } finally {
      log.debug("Fetching register details from VCCS: finish");
    }
  }

  /**
   * Method performs a call to {@link VccsRepository} to fetch cleanAirZones data and returns helper
   * collection which maps cleanAirZone ID to cleanAirZone name (e.g.
   * '1b33865b-1fcb-4d28-ac7d-d4586327de7d' => 'Birmingham').
   */
  public Map<UUID, String> getCleanAirZoneIdToCleanAirZoneNameMap() {
    Response<CleanAirZonesDto> cleanAirZonesResponse = vccsRepository.findCleanAirZonesSync();
    return cleanAirZonesResponse.body().getCleanAirZones().stream()
        .collect(Collectors.toMap(CleanAirZoneDto::getCleanAirZoneId, CleanAirZoneDto::getName));
  }
}
