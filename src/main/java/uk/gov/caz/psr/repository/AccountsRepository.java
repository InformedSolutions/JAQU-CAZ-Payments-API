package uk.gov.caz.psr.repository;

import java.io.IOException;
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
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.accounts.AccountUsersResponse;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.accounts.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.psr.dto.accounts.UpdatePaymentHistoryExportRequest;
import uk.gov.caz.psr.dto.accounts.UserDetailsResponse;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Wrapper for the Accounts Service.
 */
public interface AccountsRepository {

  /**
   * Method to create retrofit2 account service for getAccountVehicles call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/accounts/{accountId}/vehicles")
  Call<VehiclesResponseDto> getAccountVehicles(
      @Path("accountId") String accountId,
      @Query("pageNumber") String pageNumber,
      @Query("pageSize") String pageSize,
      @Query("cazId") String cazId,
      @Query("query") String query,
      @Query("onlyChargeable") String onlyChargeable,
      @Query("onlyDetermined") String onlyDetermined
  );

  /**
   * Synchronous wrapper for getAccountVehicles call.
   *
   * @param accountId  the identifier of the account
   * @param pageNumber the number of the page
   * @param pageSize   the size of the page
   * @param cazId      Clean Air Zone ID
   * @param query      part of VRN for partial search
   */
  default Response<VehiclesResponseDto> getAccountChargeableVehiclesSync(
      UUID accountId, int pageNumber, int pageSize, UUID cazId, String query) {
    try {
      return getAccountVehicles(accountId.toString(), Integer.toString(pageNumber),
          Integer.toString(pageSize), cazId.toString(), query, "true", "true").execute();
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
  Call<VehicleWithCharges> getAccountSingleVehicleVrn(
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
   *
   * @param accountId the identifier of the account
   * @param vrn       the VRN to query.
   * @return details of a single vehicle vrn.
   */
  default Response<VehicleWithCharges> getAccountSingleVehicleVrnSync(
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
   *
   * @param accountId the identifier of the account
   * @param body      DirectDebitMandate details which need to be saved in Accounts Service
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

  /**
   * Updates the payment history export job in the account microservice.
   */
  @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
  })
  @PATCH("v1/accounts/{accountId}/payment-history-export/{registerJobId}")
  Call<Void> updatePaymentHistoryExportJob(
      @Path("accountId") UUID accountId,
      @Path("registerJobId") Integer registerJobId,
      @Body UpdatePaymentHistoryExportRequest body);

  /**
   * A synchronous wrapper for {@code updatePaymentHistoryExportJob} call.
   */
  default Response<Void> updatePaymentHistoryExportJobSync(UUID accountId, Integer registerJobId,
      UpdatePaymentHistoryExportRequest body) {
    try {
      return updatePaymentHistoryExportJob(accountId, registerJobId, body).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
}
