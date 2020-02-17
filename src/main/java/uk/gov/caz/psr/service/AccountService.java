package uk.gov.caz.psr.service;

import java.io.IOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

@AllArgsConstructor
@Service
@Slf4j
public class AccountService {
  
  private final AccountsRepository accountsRepository;

  public AccountVehicleRetrievalResponse retrieveAccountVehicles(UUID accountId,
      String pageNumber, String pageSize) throws IOException {
    Response<AccountVehicleRetrievalResponse> accountsResponse = accountsRepository
        .getAccountVehicleVrns(accountId, pageNumber, pageSize)
        .execute();
    if (accountsResponse.isSuccessful()) {
      return accountsResponse.body();
    } else {
      log.error("Accounts call return error {}, code: {}", accountsResponse.errorBody(),
          accountsResponse.code());
      throw new ExternalServiceCallException();
      
    }
  }

}
