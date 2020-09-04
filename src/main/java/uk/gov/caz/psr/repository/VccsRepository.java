package uk.gov.caz.psr.repository;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.psr.dto.vccs.RegisterDetailsDto;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Retrofit2 repository to create a vccs call.
 */
@Repository
public interface VccsRepository {

  @Slf4j
  final class Logger {

  }

  /**
   * Method to create retrofit2 vccs for cleanAirZones call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/clean-air-zones")
  Call<CleanAirZonesDto> findCleanAirZones();

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/{vrn}/compliance")
  Call<ComplianceResultsDto> findCompliance(@Path("vrn") String vrn,
      @Query("zones") String zones);

  @Headers("Accept: application/json")
  @POST("v1/compliance-checker/vehicles/bulk-compliance")
  Call<List<ComplianceResultsDto>> findComplianceInBulk(@Body List<String> vrns,
      @Query("zones") String zones);

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/{vrn}/details")
  Call<VehicleDto> findVehicleDetails(@Path("vrn") String vrn);

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/unrecognised/{type}/compliance")
  Call<VehicleTypeCazChargesDto> findUnknownVehicleCompliance(
      @Path("type") String type, @Query("zones") String zones);

  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/vehicles/{vrn}/register-details")
  Call<RegisterDetailsDto> getRegisterDetails(@Path("vrn") String vrn);

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<CleanAirZonesDto> findCleanAirZonesSync() {
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
   * Wraps REST API call for finding compliance in {@link Response} by making a synchronous
   * request.
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
   * Wraps REST API call for finding compliance in bulk in {@link Response} by making a synchronous
   * request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<List<ComplianceResultsDto>> findComplianceInBulkSync(List<String> vrns,
      String zones) {
    try {
      return findComplianceInBulk(vrns, zones).execute();
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
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<RegisterDetailsDto> getRegisterDetailsSync(String vrn) {
    try {
      Logger.log.info("Begin: Fetching register details from VCCS");
      return getRegisterDetails(vrn).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching register details from VCCS");
    }
  }
}
