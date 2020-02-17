package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import retrofit2.Response;

import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse.CleanAirZoneDto;
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