package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
@Slf4j
public class VehicleComplianceRetrievalService {

  @Value("${services.connection-timeout-seconds:10}")
  private int serviceCallTimeout;
  
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
          results.add(buildExceptionResult(aom, 
              complianceResultResponse.getIdentifier().substring(6)));
        } else if (complianceResultResponse.getHttpStatus().equals(HttpStatus.NOT_FOUND)) {
          log.error("could not find vrn");
          results.add(buildExceptionResult(aom, 
              complianceResultResponse.getIdentifier().substring(6)));
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

  private ComplianceResultsDto buildExceptionResult(AsyncOperationsMatcher aom, 
      String identifier) {
    return ComplianceResultsDto.builder()
        .registrationNumber(
            aom.getValueByKey(identifier))
        .complianceOutcomes(Collections.emptyList())
        .build();
  }

  /**
   * Starts and awaits for all async requests to VCCS.
   */
  private void callVehicleComplianceChecker(
      List<AsyncOp<ComplianceResultsDto>> complianceResults) {
    try {
      asyncRestService
          .startAndAwaitAll(complianceResults, serviceCallTimeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      log.error("Unexpected exception occurs ", exception);
      throw new ExternalServiceCallException(exception.getMessage());
    }
  }

  /**
   * Helper method to get timeout seconds.
   *
   * @return timeout
   */
  long calculateTimeoutInSeconds() {
    return new Long(serviceCallTimeout);
  }
}
