package uk.gov.caz.psr.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.correlationid.MdcCorrelationIdInjector;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

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
    List<AsyncOp<ComplianceResultsDto>> complianceResultResponses = vrns.stream()
        .map(vrn -> vccsRepository
            .findComplianceAsync(vrn, zones, MdcCorrelationIdInjector.getCurrentValue()))
        .collect(Collectors.toList());

    callVehicleComplianceChecker(complianceResultResponses);
    
    for (AsyncOp<ComplianceResultsDto> complianceResultResponse : complianceResultResponses) {
      //TODO: Handle HTTP 422 errors for unknown vehicle types
      if (complianceResultResponse.hasError()) {
        log.error("VCCS call return error {}, code: {}", complianceResultResponse.getError(),
            complianceResultResponse.getHttpStatus());
        throw new ExternalServiceCallException();
      }      
    }
    
    List<ComplianceResultsDto> results = complianceResultResponses
        .stream()
        .map(complianceResultResponse -> complianceResultResponse.getResult())
        .collect(Collectors.toList());
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
