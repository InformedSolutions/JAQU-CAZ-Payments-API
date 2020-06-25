package uk.gov.caz.psr.repository;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import uk.gov.caz.psr.dto.whitelist.WhitelistedVehicleResponseDto;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * HTTP client of the whitelist service.
 */
public interface WhitelistRepository {

  /**
   * Gets details of a whitelisted vehicle by its {@code vrn} in an asynchronous manner.
   */
  @Headers("Accept: application/json")
  @GET("v1/whitelisting/vehicles/{vrn}")
  Call<WhitelistedVehicleResponseDto> getWhitelistVehicleDetails(@Path("vrn") String vrn);

  /**
   * Gets details of a whitelisted vehicle by its {@code vrn} in a synchronous manner.
   */
  default Response<WhitelistedVehicleResponseDto> getWhitelistVehicleDetailsSync(String vrn) {
    try {
      return getWhitelistVehicleDetails(vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    }
  }
}
