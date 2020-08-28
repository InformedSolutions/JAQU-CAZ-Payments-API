package uk.gov.caz.psr.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.vccs.RegisterDetailsDto;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Class responsible to call vccs for compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleComplianceRetrievalService {

  @Value("${services.connection-timeout-seconds:10}")
  private int serviceCallTimeout;

  private final VccsRepository vccsRepository;

  /**
   * Coordinate asynchronous requests to the vehicle checker to retrieve
   * compliance information on a list of VRNs.
   * 
   * @param vrns a list of vrns
   * @param zones a list of zones to check compliance for
   * @return a list of compliance results sorted by vrn
   */
  public List<ComplianceResultsDto> retrieveVehicleCompliance(List<String> vrns,
      String zones) {
    Response<List<ComplianceResultsDto>> response = vccsRepository
        .findComplianceInBulkSync(vrns, zones);
    
    if (response.isSuccessful()) {
      List<ComplianceResultsDto> results = response.body();
      results.sort(
          Comparator.comparing(ComplianceResultsDto::getRegistrationNumber));
      return results; 
    } else {
      throw new ExternalServiceCallException(
          "Vehicle Checker returned response code " + response.code());
    }
  }

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
}
