package uk.gov.caz.psr.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.controller.util.QueryStringValidator;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;
import uk.gov.caz.psr.service.AccountService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;

@AllArgsConstructor
@RestController
public class AccountsController implements AccountControllerApiSpec {

  public static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final String PAGE_NUMBER_QUERYSTRING_KEY = "pageNumber";
  private static final String PAGE_SIZE_QUERYSTRING_KEY = "pageSize";
  private static final String CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY = "cleanAirZoneId";

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
    if (queryStringValidator.queryStringInvalid("zones", queryStrings)) {
      zones = accountService.getZonesQueryStringEquivalent();
    } else {
      zones = queryStrings.get("zones");
    }

    AccountVehicleRetrievalResponse accountVehicleRetrievalResponse =
        accountService.retrieveAccountVehicles(accountId,
            queryStrings.get(PAGE_NUMBER_QUERYSTRING_KEY),
            queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));
    
    List<ComplianceResultsDto> results = new ArrayList<>();
    if (accountVehicleRetrievalResponse.getTotalVrnsCount() > 0) {
      results = vehicleComplianceRetrievalService
          .retrieveVehicleCompliance(
              accountVehicleRetrievalResponse.getVrns(),
              zones);      
    }

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
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY), Arrays.asList(PAGE_SIZE_QUERYSTRING_KEY));
    String vrn = queryStrings.get("vrn");
    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));
    String direction = checkDirectionQueryString(queryStrings.get("direction"), vrn);
    int pageSize = Integer.parseInt(queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));

    List<VrnWithTariffAndEntrancesPaid> vrnsWithTariffAndCharge = accountService
        .retrieveChargeableAccountVehicles(accountId, direction, pageSize, vrn, cleanAirZoneId);
    List<String> chargeableVrns = trimChargeableVehicles(vrnsWithTariffAndCharge, pageSize, 
        direction)
        .stream()
        .map(vrnWithTariffAndCharge -> vrnWithTariffAndCharge.getVrn())
        .collect(Collectors.toList());
    ChargeableAccountVehiclesResult vrnsAndEntrantDates = ChargeableAccountVehiclesResult.from(
        accountService.getPaidEntrantPayments(chargeableVrns, cleanAirZoneId),
        vrnsWithTariffAndCharge);

    if (vrnsAndEntrantDates.getResults().isEmpty()) {
      return ResponseEntity.ok().body(
          ChargeableAccountVehicleResponse.builder().firstVrn(null).lastVrn(null)
              .chargeableAccountVehicles(vrnsAndEntrantDates).build());
    }

    return ResponseEntity.ok()
        .body(createResponseFromChargeableAccountVehicles(vrnsWithTariffAndCharge,
            vrnsAndEntrantDates, direction, pageSize, !StringUtils.hasText(vrn)));
  }

  private String checkDirectionQueryString(String direction, String vrn) {
    if (StringUtils.hasText(direction) && !direction.equals("next")
        && !direction.equals("previous")) {
      throw new InvalidRequestPayloadException(
          "Direction supplied must be one of either 'next' or 'previous'.");
    }

    if (StringUtils.hasText(direction) && direction.equals("previous")
        && !StringUtils.hasText(vrn)) {
      throw new InvalidRequestPayloadException(
          "Direction cannot be set to 'previous' if no VRN has been provided.");
    }
    return direction;
  }

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveSingleChargeableVehicle(
      UUID accountId, String vrn, Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings,
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY), null);
    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));

    ChargeableAccountVehiclesResult vrnsAndEntryDates = accountService
        .retrieveSingleChargeableAccountVehicle(accountId, vrn, cleanAirZoneId);

    ChargeableAccountVehicleResponse response = ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(vrnsAndEntryDates)
        .build();

    return ResponseEntity.ok()
        .body(response);
  }

  private List<VrnWithTariffAndEntrancesPaid> trimChargeableVehicles(
      List<VrnWithTariffAndEntrancesPaid> chargeableVrns, int pageSize, String direction) {
    return chargeableVrns.size() > pageSize ? chargeableVrns.subList(0, pageSize) : chargeableVrns;
  }

  private ChargeableAccountVehicleResponse createResponseFromChargeableAccountVehicles(
      List<VrnWithTariffAndEntrancesPaid> vrnsWithTariffAndCharge,
      ChargeableAccountVehiclesResult results, String direction,
      int pageSize, boolean firstPage) {
    String firstVrn = firstPage ? null : results.getResults().get(0).getVrn();
    String lastVrn = results.getResults().get(results.getResults().size() - 1).getVrn();
    String travelDirection = StringUtils.hasText(direction) ? direction : "next";
    if (vrnsWithTariffAndCharge.size() < pageSize + 1) {
      firstVrn = travelDirection.equals("previous") ? null : firstVrn;
      lastVrn = travelDirection.equals("next") ? null : lastVrn;
    }

    return ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(results)
        .firstVrn(firstVrn)
        .lastVrn(lastVrn)
        .build();
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
