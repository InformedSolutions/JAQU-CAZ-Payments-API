package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.util.AsyncOperationsMatcher;

/**
 * Class responsible to call vccs for compliance.
 */
@Service
@AllArgsConstructor
@Slf4j
public class VehicleComplianceRetrievalService {

  private final VccsRepository vccsRepository;
  private final AsyncRestService asyncRestService;

  /**
   * Coordinate asynchronous requests to the vehicle checker to retrieve compliance
   * information on a list of VRNs.
   * @param vrns a list of vrns
   * @param zones a list of zones to check compliance for
   * @return a list of compliance results sorted by vrn
   */
  public List<ComplianceResultsDto> retrieveVehicleCompliance(List<String> vrns, String zones) {
    AsyncOperationsMatcher aom = new AsyncOperationsMatcher(vrns);
    
    List<AsyncOp<ComplianceResultsDto>> complianceResultResponses = aom.getEntrySet()
        .stream()
        .map(e -> vccsRepository
            .findComplianceAsync(e.getValue(), zones, e.getKey()))
        .collect(Collectors.toList());

    callVehicleComplianceChecker(complianceResultResponses);
    
    List<ComplianceResultsDto> results = new ArrayList<ComplianceResultsDto>();
    
    for (AsyncOp<ComplianceResultsDto> complianceResultResponse : complianceResultResponses) {
      if (complianceResultResponse.hasError()) {
        if (complianceResultResponse.getHttpStatus().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
          log.error("could not get vehicle compliance due to null vehicle type");
          results.add(ComplianceResultsDto.builder()
              .registrationNumber(aom.getVrnByIdentifier(complianceResultResponse.getIdentifier()))
              .build());
        } else {
          log.error("VCCS call return error {}, code: {}", complianceResultResponse.getError(),
              complianceResultResponse.getHttpStatus());
          throw new ExternalServiceCallException();          
        }
      } else {
        results.add(complianceResultResponse.getResult());
      }
    }
    
    results.sort(Comparator.comparing(ComplianceResultsDto:: getRegistrationNumber));
    
    return results;
  }

  /**
   * Starts and awaits for all async requests to VCCS.
   */
  private void callVehicleComplianceChecker(
      List<AsyncOp<ComplianceResultsDto>> complianceResults) {
    long timeout = calculateTimeoutInSeconds();
    try {
      asyncRestService
          .startAndAwaitAll(complianceResults, timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      log.error("Unexpected exception occurs ", exception);
      throw new ExternalServiceCallException(exception);
    }
  }

  /**
   * Helper method to get timeout seconds.
   *
   * @return timeout
   */
  static long calculateTimeoutInSeconds() {
    return 8L;
  }
}
