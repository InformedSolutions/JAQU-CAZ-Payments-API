package uk.gov.caz.psr.util;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.CachedCharge;
import uk.gov.caz.psr.dto.VehicleDetails;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;

/**
 * {@code AccountVehicleRetrievalResponse} to {@code VehicleRetrievalResponseDto} converter.
 */
@Component
public class AccountVehicleRetrievalConverter {

  /**
   * Converts the passed {@code AccountVehicleRetrievalResponse} to an instance of {@link
   * VehicleRetrievalResponseDto}.
   */
  public VehicleRetrievalResponseDto toVehicleRetrievalResponseDto(
      AccountVehicleRetrievalResponse accountVehicleRetrievalResponse, String pageNumber,
      String perPage) {
    Preconditions.checkNotNull(accountVehicleRetrievalResponse,
        "accountVehicleRetrievalResponse cannot be null");

    return VehicleRetrievalResponseDto.builder()
        .vehicles(
            complianceResultsFromVehicleDetails(accountVehicleRetrievalResponse.getVehicles()))
        .page(Integer.parseInt(pageNumber))
        .pageCount(accountVehicleRetrievalResponse.getPageCount())
        .perPage(Integer.parseInt(perPage))
        .totalVrnsCount(accountVehicleRetrievalResponse.getTotalVehiclesCount())
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
      List<VehicleDetails> vehicles) {
    return vehicles.stream()
        .map(vehicle -> ComplianceResultsDto.builder()
            .registrationNumber(vehicle.getVrn())
            .isRetrofitted(vehicle.getIsRetrofitted())
            .isExempt(vehicle.getIsExempt())
            .vehicleType(vehicle.getVehicleType())
            .complianceOutcomes(complianceOutcomesFromCachedCharges(vehicle.getCachedCharges()))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Converts cachedCharges from the Accounts API to {@code ComplianceOutcomeDto}.
   */
  private List<ComplianceOutcomeDto> complianceOutcomesFromCachedCharges(
      List<CachedCharge> cachedCharges) {
    return cachedCharges.stream()
        .map(cachedCharge -> ComplianceOutcomeDto.builder()
            .cleanAirZoneId(cachedCharge.getCazId())
            .charge(cachedCharge.getCharge())
            .tariffCode(cachedCharge.getTariffCode())
            .build())
        .collect(Collectors.toList());
  }
}
