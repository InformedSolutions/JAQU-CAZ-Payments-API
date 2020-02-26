package uk.gov.caz.psr.service;

import com.amazonaws.util.StringUtils;
import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse.CleanAirZoneDto;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.AccountNotFoundException;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Service to interact with the Accounts Service.
 */
@AllArgsConstructor
@Service
public class AccountService {
  
  private final AccountsRepository accountsRepository;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;
  private final VccsRepository vccRepository;
  
  /**
   * Retrieve a page of VRNs of vehicles associated with a given account ID.
   * @param accountId the id of the account
   * @param pageNumber the number of the page
   * @param pageSize the size of the page
   * @return
   */
  public AccountVehicleRetrievalResponse retrieveAccountVehicles(UUID accountId,
      String pageNumber, String pageSize) {
    Response<AccountVehicleRetrievalResponse> accountsResponse = accountsRepository
        .getAccountVehicleVrnsSync(accountId, pageNumber, pageSize);
    if (accountsResponse.isSuccessful()) {
      return accountsResponse.body();
    } else {
      switch (accountsResponse.code()) {
        case 404:
          throw new AccountNotFoundException();
        default:
          throw new ExternalServiceCallException();          
      }        
    }
  }
  
  /**
   * Fetches a list of vehicles from the Accounts Service and lazily checks their
   * chargeability until it generates a full list of results.
   * @param accountId the account whose vehicles should be returned
   * @param direction 'next' or 'previous' in terms of pages
   * @param pageSize the size of the list to be returned
   * @param vrn the "cursor" on which to search the account vehicles
   * @param cleanAirZoneId the Clean Air Zone to check compliance for
   * @return a list of chargeable VRNs
   */
  public PaidPaymentsResponse retrieveChargeableAccountVehicles(UUID accountId, String direction, 
      int pageSize, String vrn, String cleanAirZoneId) {
    List<String> results = new ArrayList<String>();
    Boolean lastPage = false;
    // initialise cursor at first VRN
    String vrnCursor = vrn;
    
    while (results.size() < (pageSize + 1) && !lastPage) {
      // get triple the number of vrns as will be on page to reduce overall request numbers
      List<String> accountVrns = getAccountVrns(accountId, direction, pageSize * 3, vrnCursor);
      
      // check if the end of pages has been reached, if not set new cursor
      if (accountVrns.size() < pageSize) {
        lastPage = true;
      } else {
        vrnCursor = getVrnCursor(accountVrns, direction);
      }      
      
      List<String> chargeableVrns = getChargeableVrnsFromVcc(accountVrns, cleanAirZoneId);
      
      if (! chargeableVrns.isEmpty()) {
        results.addAll(chargeableVrns);
      }
    }
    
    return PaidPaymentsResponse.from(getPaidEntrantPayments(results, cleanAirZoneId));
  }
  
  private Map<String, List<EntrantPayment>> getPaidEntrantPayments(
      List<String> results, String cleanAirZoneId) {
    return getPaidEntrantPaymentsService.getResults(
        new HashSet<String>(results),
        LocalDate.now().minusDays(6),
        LocalDate.now().plusDays(6),
        UUID.fromString(cleanAirZoneId));
  }

  private List<String> getChargeableVrnsFromVcc(List<String> accountVrns, 
      String cleanAirZoneId) {
    List<ComplianceResultsDto> complianceOutcomes = vehicleComplianceRetrievalService
        .retrieveVehicleCompliance(accountVrns, cleanAirZoneId);
    return complianceOutcomes
        .stream()
        .filter(complianceOutcome -> vrnIsChargeable(complianceOutcome))
        .map(complianceOutcome -> complianceOutcome.getRegistrationNumber())
        .collect(Collectors.toList());
  }

  private List<String> getAccountVrns(UUID accountId, String direction, int pageSize, 
      String vrnCursor) {
    Response<List<String>> accountsResponse = accountsRepository
        .getAccountVehicleVrnsByCursorSync(accountId, direction, pageSize, vrnCursor);
    return accountsResponse.body();
  }

  private String getVrnCursor(List<String> accountVrns, String direction) {
    Collections.sort(accountVrns);
    if (direction.equals("next")) {
      return accountVrns.get(accountVrns.size() - 1);
    } else if (direction.equals("previous")) {
      return accountVrns.get(0);
    } else if (StringUtils.isNullOrEmpty(direction)) {
      // assume 'next' if direction not set
      return accountVrns.get(accountVrns.size() - 1);
    } else { 
      throw new IllegalArgumentException("Direction given is invalid.");
    }
  }

  private Boolean vrnIsChargeable(ComplianceResultsDto complianceOutcome) {
    Preconditions.checkArgument(complianceOutcome.getComplianceOutcomes().size() <= 1);
    if (complianceOutcome.getComplianceOutcomes().isEmpty()) {
      return true;
    } else {
      float charge = complianceOutcome.getComplianceOutcomes().get(0).getCharge();
      return charge > 0;      
    }
  }

  /**
   * Helper method for retrieving a list of comma delimited clean air zones IDs.
   * @return a list of comma delimited clean air zones IDs.
   */
  public String getZonesQueryStringEquivalent() {
    Response<CleanAirZonesResponse> zones = vccRepository.findCleanAirZonesSync(); 
    List<CleanAirZoneDto> cleanAirZoneDtos = zones.body().getCleanAirZones();
    List<String> mappedZoneIds = new ArrayList<String>();

    for (CleanAirZoneDto dto : cleanAirZoneDtos) {
      mappedZoneIds.add(dto.getCleanAirZoneId().toString());
    }

    return String.join(",", mappedZoneIds);
  }
  
}