package uk.gov.caz.psr.repository;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Retrofit2 repository to create a vccs call.
 */
@Repository
public interface VccsRepository {

  @Slf4j
  final class Logger {}
  
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

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/{vrn}/details")
  Call<VehicleDto> findVehicleDetails(@Path("vrn") String vrn);

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/unrecognised/{type}/compliance")
  Call<VehicleTypeCazChargesDto> findUnknownVehicleCompliance(
      @Path("type") String type, @Query("zones") String zones);

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<CleanAirZonesResponse> findCleanAirZonesSync() {
    try {
      Logger.log.info("Begin: Fetching clean air zones from VCCS");
      return findCleanAirZones().execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching clean air zones from VCCS");
    }
  }

  /**
   * Wraps REST API call for finding compliance in {@link Response} by making a
   * synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<ComplianceResultsDto> findComplianceSync(String vrn,
      String zones) {
    try {
      Logger.log.info("Begin: Fetching compliance result from VCCS");
      return findCompliance(vrn, zones).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching compliance result from VCCS");
    }
  }

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<VehicleDto> findVehicleDetailsSync(String vrn) {
    try {
      Logger.log.info("Begin: Fetching vehicle details from VCCS");
      return findVehicleDetails(vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching vehicle details from VCCS");
    }
  }

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<VehicleTypeCazChargesDto> findUnknownVehicleComplianceSync(
      String type, String zones) {
    try {
      Logger.log.info("Begin: Fetching unknown vehicle compliance result from VCCS");
      return findUnknownVehicleCompliance(type, zones).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching unknown vehicle compliance result from VCCS");
    }
  }

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param vrn the vrn to find compliance of
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<ComplianceResultsDto> findComplianceAsync(String vrn,
      String zones, String identifier) {
    return AsyncOp.from("VCCS: " + identifier, findCompliance(vrn, zones));
  }
}
