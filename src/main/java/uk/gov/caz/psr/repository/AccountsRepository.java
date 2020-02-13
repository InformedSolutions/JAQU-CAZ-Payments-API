package uk.gov.caz.psr.repository;

import java.util.UUID;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;

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
}
