package uk.gov.caz.psr.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Service to interact with the Accounts Service.
 */
@AllArgsConstructor
@Service
public class AccountService {
  
  private final AccountsRepository accountsRepository;

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
      throw new ExternalServiceCallException();
    }
  }
}