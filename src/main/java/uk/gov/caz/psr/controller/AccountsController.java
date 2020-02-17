package uk.gov.caz.psr.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;
import uk.gov.caz.psr.service.AccountService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;

@AllArgsConstructor
@RestController
public class AccountsController implements AccountControllerApiSpec {

  public static final String ACCOUNTS_PATH = "/v1/accounts";
  
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;
  private final AccountService accountService;

  @Override
  public ResponseEntity<VehicleRetrievalResponseDto> retrieveVehiclesAndCharges(
      UUID accountId, Map<String, String> queryStrings) {
    
    validateRequest(queryStrings);
    
    String zones = "";
    
    // If no collection of zones has been passed via querystring, 
    // retrieve all zones from service layer.
    if (queryStringAbsent("zones", queryStrings)) {
      zones = accountService.getZonesQueryStringEquivalent();
    } else {
      zones = queryStrings.get("zones");
    }
    
    AccountVehicleRetrievalResponse accountVehicleRetrievalResponse = 
        accountService.retrieveAccountVehicles(accountId, queryStrings.get("pageNumber"),
            queryStrings.get("pageSize"));
    
    List<ComplianceResultsDto> results = vehicleComplianceRetrievalService
        .retrieveVehicleCompliance(
        accountVehicleRetrievalResponse.getVrns(), 
        zones);
    
    return ResponseEntity.ok().body(createResponseFromVehicleComplianceRetrievalResults(
        results, queryStrings.get("pageNumber"), queryStrings.get("pageSize"),
        accountVehicleRetrievalResponse.getPageCount(), 
        accountVehicleRetrievalResponse.getTotalVrnsCount()));
  }

  /**
   * Helper method to test presence of required querystring parameters.
   * @param map querystring parameter map.
   */
  private void validateRequest(Map<String, String> map) {
    if (map.size() < 2 || queryStringAbsent("pageNumber", map)
        || queryStringAbsent("pageSize", map)) {
      throw new InvalidRequestPayloadException(
          "Please supply 'pageNumber' and 'pageSize' query strings.");
    }
  }
  
  private Boolean queryStringAbsent(String key, Map<String, String> map) {
    return !map.containsKey(key) || !StringUtils.hasText(map.get(key));
  }

  private VehicleRetrievalResponseDto createResponseFromVehicleComplianceRetrievalResults(
      List<ComplianceResultsDto> results, String pageNumber, String pageSize, 
      int pageCount, long totalVrnsCount) {
    return VehicleRetrievalResponseDto.builder()
        .vehicles(results)
        .page(Integer.parseInt(pageNumber))
        .pageCount(pageCount)
        .perPage(Integer.parseInt(pageSize))
        .totalVrnsCount(totalVrnsCount)
        .build();
  }

}
