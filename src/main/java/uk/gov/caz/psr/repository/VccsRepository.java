package uk.gov.caz.psr.repository;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Retrofit2 repository to create a vccs call.
 */
public interface VccsRepository {
  
  /**
   * Method to create retrofit2 vccs for cleanAirZones call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/clean-air-zones")
  Call<CleanAirZonesResponse> findCleanAirZones();

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/{vrn}/compliance")
  Call<ComplianceResultsDto> findCompliance(@Path("vrn") String vrn, 
      @Query("zones") String zones);

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<CleanAirZonesResponse> findCleanAirZonesSync() {
    try {
      return findCleanAirZones().execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }

  /**
   * Wraps REST API call for finding compliance in {@link Response} by making a synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<ComplianceResultsDto> findComplianceSync(String vrn, String zones) {
    try {
      return findCompliance(vrn, zones).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
  
  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param vrn the vrn to find compliance of
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<ComplianceResultsDto> findComplianceAsync(
      String vrn, String zones, String identifier) {
    return AsyncOp.from("VCCS: " + identifier, findCompliance(vrn, zones));
  }
}
