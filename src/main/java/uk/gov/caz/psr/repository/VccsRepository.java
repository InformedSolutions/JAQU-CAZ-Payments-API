package uk.gov.caz.psr.repository;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
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
}
