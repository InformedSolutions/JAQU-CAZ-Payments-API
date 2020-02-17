package uk.gov.caz.psr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import retrofit2.Response;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse.CleanAirZoneDto;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;

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
    return accountsRepository.getAccountVehicleVrnsSync(accountId, pageNumber, pageSize)
        .body();
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