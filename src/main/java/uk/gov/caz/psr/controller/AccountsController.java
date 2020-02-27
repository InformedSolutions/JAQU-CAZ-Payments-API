package uk.gov.caz.psr.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.controller.util.QueryStringValidator;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;
import uk.gov.caz.psr.service.AccountService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;

@AllArgsConstructor
@RestController
public class AccountsController implements AccountControllerApiSpec {

  public static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final String PAGE_NUMBER_QUERYSTRING_KEY = "pageNumber";
  private static final String PAGE_SIZE_QUERYSTRING_KEY = "pageSize";
  
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;
  private final AccountService accountService;
  private final QueryStringValidator queryStringValidator;

  @Override
  public ResponseEntity<VehicleRetrievalResponseDto> retrieveVehiclesAndCharges(
      UUID accountId, Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings, 
        Collections.emptyList(),
        Arrays.asList(PAGE_NUMBER_QUERYSTRING_KEY, PAGE_SIZE_QUERYSTRING_KEY));

    String zones = "";
    
    // If no collection of zones has been passed via querystring, 
    // retrieve all zones from service layer.
    if (queryStringAbsent("zones", queryStrings)) {
      zones = accountService.getZonesQueryStringEquivalent();
    } else {
      zones = queryStrings.get("zones");
    }
    
    AccountVehicleRetrievalResponse accountVehicleRetrievalResponse =
        accountService.retrieveAccountVehicles(accountId,
            queryStrings.get(PAGE_NUMBER_QUERYSTRING_KEY),
            queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));

    List<ComplianceResultsDto> results = vehicleComplianceRetrievalService
        .retrieveVehicleCompliance(
        accountVehicleRetrievalResponse.getVrns(), 
        zones);

    return ResponseEntity.ok()
        .body(createResponseFromVehicleComplianceRetrievalResults(results,
            queryStrings.get(PAGE_NUMBER_QUERYSTRING_KEY),
            queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY),
            accountVehicleRetrievalResponse.getPageCount(),
            accountVehicleRetrievalResponse.getTotalVrnsCount()));
  }

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveChargeableVehicles(UUID accountId,
      Map<String, String> queryStrings) {
    
    queryStringValidator.validateRequest(queryStrings, 
        Arrays.asList("cleanAirZoneId"), Arrays.asList(PAGE_SIZE_QUERYSTRING_KEY));
    String cleanAirZoneId = queryStrings.get("cleanAirZoneId");
    String direction = queryStrings.get("direction");
    int pageSize = Integer.parseInt(queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));
    String vrn = queryStrings.get("vrn");
    
    List<String> chargeableVrns = accountService.retrieveChargeableAccountVehicles(
        accountId, direction, pageSize, vrn, cleanAirZoneId);
    PaidPaymentsResponse vrnsAndEntrantDates = PaidPaymentsResponse.from(
        accountService.getPaidEntrantPayments(trimChargeableVehicles(chargeableVrns, pageSize), 
            cleanAirZoneId));
    
    return ResponseEntity.ok()
        .body(createResponseFromChargeableAccountVehicles(chargeableVrns, vrnsAndEntrantDates, 
            direction, pageSize, vrn == null));
  }

  private List<String> trimChargeableVehicles(List<String> chargeableVrns, int pageSize) {
    return chargeableVrns.size() > pageSize ? chargeableVrns.subList(0, pageSize) : chargeableVrns;
  }

  private ChargeableAccountVehicleResponse createResponseFromChargeableAccountVehicles(
      List<String> chargeableVrns, PaidPaymentsResponse results, String direction, int pageSize, 
      boolean firstPage) {
    String firstVrn = firstPage ? null : results.getResults().get(0).getVrn();
    String lastVrn = results.getResults().get(results.getResults().size() - 1).getVrn();
    String travelDirection = StringUtils.hasText(direction) ? direction : "next"; 
    if (chargeableVrns.size() < pageSize + 1) {
      firstVrn = travelDirection.equals("previous") ? null : firstVrn;
      lastVrn = travelDirection.equals("next") ? null : lastVrn;
    }
    
    return ChargeableAccountVehicleResponse
      .builder()
      .paidPayments(results)
      .firstVrn(firstVrn)
      .lastVrn(lastVrn)
      .build();
  }

  /**
   * Helper method to test for present of a key in a querystring parameter.
   * @param key the key to search for.
   * @param map the collection of keys to query against. 
   * @return Boolean indicator as to whether the key exists in the map.
   */
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

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveSingleChargeableVehicle(
      UUID accountId, String vrn,Map<String, String> queryStrings) {
    
    return null;
  }
}
