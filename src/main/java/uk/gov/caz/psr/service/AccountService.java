package uk.gov.caz.psr.service;

import java.io.IOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.repository.AccountsRepository;

@AllArgsConstructor
@Service
public class AccountService {
  
  private final AccountsRepository accountsRepository;

  public AccountVehicleRetrievalResponse retrieveAccountVehicles(UUID accountId,
      String pageNumber, String pageSize) throws IOException {
    return accountsRepository.getAccountVehicleVrns(accountId, pageNumber, pageSize).execute()
        .body();
  }

}
