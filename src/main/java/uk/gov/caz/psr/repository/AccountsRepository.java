package uk.gov.caz.psr.repository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.AccountVehicleResponse;
import uk.gov.caz.psr.dto.accounts.AccountUsersResponse;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.accounts.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.psr.dto.accounts.UserDetailsResponse;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Wrapper for the Accounts Service.
 */
public interface AccountsRepository {

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
  Call<AccountVehicleResponse> getAccountSingleVehicleVrn(
      @Path("accountId") UUID accountId, @Path("vrn") String vrn
    );

  /**
   * Gets a list of mandates for the account by its identifier ({@code accountId}).
   */
  @Headers("Accept: application/json")
  @GET("v1/accounts/{accountId}/direct-debit-mandates")
  Call<AccountDirectDebitMandatesResponse> getAccountDirectDebitMandates(
      @Path("accountId") UUID accountId);

  /**
   * Synchronously gets a list of mandates for the account by its identifier ({@code accountId}).
   */
  default Response<AccountDirectDebitMandatesResponse> getAccountDirectDebitMandatesSync(
      UUID accountId) {
    try {
      return getAccountDirectDebitMandates(accountId).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Synchronous wrapper for getAccountVehicleVrns call.
   * @param accountId the identifier of the account
   * @param vrn the VRN to query.
   * @return details of a single vehicle vrn.
   */
  default Response<AccountVehicleResponse> getAccountSingleVehicleVrnSync(
      UUID accountId, String vrn) {
    try {
      return getAccountSingleVehicleVrn(accountId, vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Method to create DirectDebitMandate for account.
   *
   * @return {@link Call}
   */
  @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
  })
  @POST("v1/accounts/{accountId}/direct-debit-mandates")
  Call<CreateDirectDebitMandateResponse> createDirectDebitMandate(
      @Path("accountId") UUID accountId, @Body CreateDirectDebitMandateRequest body);

  /**
   * Synchronous wrapper for createDirectDebitMandate call.
   * @param accountId the identifier of the account
   * @param body DirectDebitMandate details which need to be saved in Accounts Service
   * @return details of a created DirectDebitMandate from AccountService
   */
  default Response<CreateDirectDebitMandateResponse> createDirectDebitMandateSync(
      UUID accountId, CreateDirectDebitMandateRequest body) {
    try {
      return createDirectDebitMandate(accountId, body).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Updates mandates in the account microservice.
   */
  @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
  })
  @PATCH("v1/accounts/{accountId}/direct-debit-mandates")
  Call<Void> updateDirectDebitMandates(@Path("accountId") UUID accountId,
      @Body DirectDebitMandatesUpdateRequest body);

  /**
   * A synchronous wrapper for {@code updateDirectDebitMandates} call.
   */
  default Response<Void> updateDirectDebitMandatesSync(UUID accountId,
      DirectDebitMandatesUpdateRequest body) {
    try {
      return updateDirectDebitMandates(accountId, body).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Method to get all users associated with the provided account.
   */
  @Headers({
      "Accept: application/json"
  })
  @GET("v1/accounts/{accountId}/users")
  Call<AccountUsersResponse> getAllUsers(@Path("accountId") UUID accountId);

  /**
   * A synchronous wrapper for {@code getAllUsers} call.
   */
  default Response<AccountUsersResponse> getAllUsersSync(UUID accountId) {
    try {
      return getAllUsers(accountId).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Method to get information about user.
   */
  @Headers({
      "Accept: application/json"
  })
  @GET("v1/users/{userId}")
  Call<UserDetailsResponse> getUserDetails(@Path("userId") UUID userId);

  /**
   * A synchronous wrapper for {@code getUser} call.
   */
  default Response<UserDetailsResponse> getUserDetailsSync(UUID userId) {
    try {
      return getUserDetails(userId).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
}
