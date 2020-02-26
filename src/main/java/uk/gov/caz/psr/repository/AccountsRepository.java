package uk.gov.caz.psr.repository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

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
      return getAccountVehicleVrns(accountId, pageNumber, pageSize).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
  
  /**
   * Method to create retrofit2 account service for getAccountVehicleVrnsByCursor call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/accounts/{accountId}/vehicles/sorted-page")
  Call<List<String>> getAccountVehicleVrnsByCursor(
      @Path("accountId") String accountId,
      @Query("direction") String direction,
      @Query("pageSize") String pageSize,
      @Query("vrn") String vrn
    );
  
  /**
   * Synchronous wrapper for getAccountVehicleVrnsByCursor call.
   * @param accountId the identifier of the account
   * @param pageSize the size of the page
   * @return
   */
  default Response<List<String>> getAccountVehicleVrnsByCursorSync(
      UUID accountId, String direction, int pageSize, String vrn) {
    try {
      return getAccountVehicleVrnsByCursor(accountId.toString(), direction, 
          Integer.toString(pageSize), vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
  
  /**
   * Method to create retrofit2 account service for get account vehicle call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/accounts/{accountId}/vehicles/{vrn}")
  Call<AccountVehicleRetrievalResponse> getAccountSingleVehicleVrn(
      @Path("accountId") UUID accountId, @Path("vrn") String vrn
    );
  
  /**
   * Synchronous wrapper for getAccountVehicleVrns call.
   * @param accountId the identifier of the account
   * @param vrn the VRN to query.
   * @return details of a single vehicle vrn.
   */
  default Response<AccountVehicleRetrievalResponse> getAccountSingleVehicleVrnSync(
      UUID accountId, String vrn) {
    try {
      return getAccountSingleVehicleVrn(accountId, vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
}
