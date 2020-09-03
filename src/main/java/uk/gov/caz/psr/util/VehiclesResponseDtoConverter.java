package uk.gov.caz.psr.util;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;

/**
 * {@code AccountVehicleRetrievalResponse} to {@code VehicleRetrievalResponseDto} converter.
 */
@Component
public class VehiclesResponseDtoConverter {

  /**
   * Converts the passed {@code AccountVehicleRetrievalResponse} to an instance of {@link
   * VehicleRetrievalResponseDto}.
   */
  public VehicleRetrievalResponseDto toVehicleRetrievalResponseDto(
      VehiclesResponseDto vehiclesResponse, String pageNumber,
      String perPage) {
    Preconditions.checkNotNull(vehiclesResponse,
        "vehiclesResponse cannot be null");

    return VehicleRetrievalResponseDto.builder()
        .vehicles(
            complianceResultsFromVehicleDetails(vehiclesResponse.getVehicles()))
        .page(Integer.parseInt(pageNumber))
        .pageCount(vehiclesResponse.getPageCount())
        .perPage(Integer.parseInt(perPage))
        .totalVrnsCount(vehiclesResponse.getTotalVehiclesCount())
        .build();
  }

  /**
   * Converts vehicles from the {@code AccountVehicleRetrievalResponse} to {@code
   * ComplianceResultsDto}.
   *
   * @param vehicles VehicleDetails from the Accounts API
   * @return list of {@code ComplianceResultsDto}
   */
  private List<ComplianceResultsDto> complianceResultsFromVehicleDetails(
      List<VehicleWithCharges> vehicles) {
    return vehicles.stream()
        .map(vehicle -> ComplianceResultsDto.builder()
            .registrationNumber(vehicle.getVrn())
            .isRetrofitted(vehicle.isRetrofitted())
            .isExempt(vehicle.isExempt())
            .vehicleType(vehicle.getVehicleType())
            .complianceOutcomes(complianceOutcomesFromCachedCharges(vehicle.getCachedCharges()))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Converts cachedCharges from the Accounts API to {@code ComplianceOutcomeDto}.
   */
  private List<ComplianceOutcomeDto> complianceOutcomesFromCachedCharges(
      List<VehicleCharge> cachedCharges) {
    return cachedCharges.stream()
        .map(cachedCharge -> ComplianceOutcomeDto.builder()
            .cleanAirZoneId(cachedCharge.getCazId())
            .charge(cachedCharge.getCharge().floatValue())
            .tariffCode(cachedCharge.getTariffCode())
            .build())
        .collect(Collectors.toList());
  }
}
