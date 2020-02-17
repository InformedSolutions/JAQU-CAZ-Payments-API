package uk.gov.caz.psr.repository;

import java.io.IOException;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;

/**
 * Wrapper for the Accounts Service.
 */
public interface AccountsRepository {
  
  /**
   * Method to create retrofit2 account service for getAccountVehicleVrns call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/accounts/{accountId}/vehicles")
  Call<AccountVehicleRetrievalResponse> getAccountVehicleVrns(
      @Path("accountId") UUID accountId,
      @Query("pageNumber") String pageNumber,
      @Query("pageSize") String pageSize
    );
  
  /**
   * Synchronous wrapper for getAccountVehicleVrns call.
   * @param accountId the identifier of the account
   * @param pageNumber the number of the page
   * @param pageSize the size of the page
   * @return
   */
  default Response<AccountVehicleRetrievalResponse> getAccountVehicleVrnsSync(
      UUID accountId, String pageNumber, String pageSize) {
    try {
      return getAccountVehicleVrns(accountId, pageSize, pageSize).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
