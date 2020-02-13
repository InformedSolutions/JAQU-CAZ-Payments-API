package uk.gov.caz.psr.repository;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;

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
      @Query("zones") String zones,
      @Header("X-Correlation-ID") String correlationId);


  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<CleanAirZonesResponse> findCleanAirZonesSync() {
    try {
      return findCleanAirZones().execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param correlationId for correlation
   * @param vrn the vrn to find compliance of
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<ComplianceResultsDto> findComplianceAsync(String vrn, String zones,
      String correlationId) {
    return AsyncOp.from("VCCS: " + correlationId, findCompliance(vrn, zones, correlationId));
  }
}
