package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;

/**
 * Class that represents the returned JSON when client asks for account vehicles 
 * that are chargeable.
 */
@Value
@Builder
public class ChargeableAccountVehiclesResult {

  /**
   * A list of vrns with associated charge, tariff code and paid travel dates.
   */
  @ApiModelProperty(value = 
      "${swagger.model.descriptions.chargeable-account-vehicles-result.results}")
  List<VrnWithTariffAndEntrancesPaid> results;

  /**
   * Converts provided collection to {@link ChargeableAccountVehiclesResult}.
   * @param vrnsWithTariffAndCharge list of {@link VrnWithTariffAndEntrancesPaid}
   */
  public static ChargeableAccountVehiclesResult from(Map<String, List<EntrantPayment>> results, 
      List<VrnWithTariffAndEntrancesPaid> vrnsWithTariffAndCharge) {
    List<VrnWithTariffAndEntrancesPaid> mappedResults = vrnsWithTariffAndCharge
        .stream()
        .filter(obj -> results.get(obj.getVrn()) != null)
        .map(obj -> enhanceResultWithEntrantPayments(obj, results.get(obj.getVrn())))
        .sorted(Comparator.comparing(VrnWithTariffAndEntrancesPaid::getVrn))
        .collect(Collectors.toList());

    return ChargeableAccountVehiclesResult.builder().results(mappedResults).build();
  }
  
  /**
   * Builds {@link VrnWithTariffAndEntrancesPaid} based on compliance outcome.
   * @param complianceOutcome object containing vrn, charge and tariff
   */
  public static VrnWithTariffAndEntrancesPaid buildVrnWithTariffAndEntrancesPaidFrom(
      ComplianceResultsDto complianceOutcome, UUID cleanAirZoneId) {
    ComplianceOutcomeDto zoneOutcome = complianceOutcome.getComplianceOutcomes()
        .stream()
        .filter(outcome -> outcome.getCleanAirZoneId().equals(cleanAirZoneId))
        .findFirst()
        .orElseThrow(() -> new CleanAirZoneNotFoundException(cleanAirZoneId));
    return VrnWithTariffAndEntrancesPaid.builder()
        .vrn(complianceOutcome.getRegistrationNumber())
        .charge(zoneOutcome.getCharge())
        .tariffCode(zoneOutcome.getTariffCode())
        .build();
  }
  
  /**
   * Enhances a given result with entrant payments.
   */
  private static VrnWithTariffAndEntrancesPaid enhanceResultWithEntrantPayments(
      VrnWithTariffAndEntrancesPaid instance, List<EntrantPayment> entrantPayments) {
    return instance.toBuilder()
        .paidDates(collectTravelDates(entrantPayments))
        .build();
  }

  /**
   * Maps list of {@link EntrantPayment} to list of its travel date.
   */
  private static List<LocalDate> collectTravelDates(List<EntrantPayment> entrantPayments) {
    return entrantPayments
        .stream()
        .map(EntrantPayment::getTravelDate)
        .collect(Collectors.toList());
  }

  @Value
  @Builder(toBuilder = true)
  public static class VrnWithTariffAndEntrancesPaid {

    /**
     * A VRN for which the list of paid days is being returned.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.vrn}")
    String vrn;
    
    /**
     * The tariff code for the vehicle in a given Clean Air Zone.
     */
    @ApiModelProperty(value = 
        "${swagger.model.descriptions.vrn-with-tariff-entrances.tariff-code}")
    String tariffCode;
    
    /**
     * The charge incurred by the vehicle in a given Clean Air Zone.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.charge}")
    double charge;

    /**
     * A list of days which are already paid.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.vrn-with-tariff-entrances.paid-dates}")
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    List<LocalDate> paidDates;
  }
  
}
