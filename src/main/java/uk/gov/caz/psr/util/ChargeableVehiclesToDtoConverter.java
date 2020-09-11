package uk.gov.caz.psr.util;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.model.ChargeableVehicle;

/**
 * Model-to-dto-converter for a list of {@link ChargeableVehicle}.
 */
@Component
public class ChargeableVehiclesToDtoConverter {

  private static final String DIRECTION_PREVIOUS = "previous";
  private static final String DIRECTION_NEXT = "next";

  /**
   * Converts the passed variables to an instance of {@link ChargeableAccountVehicleResponse}.
   */
  public ChargeableAccountVehicleResponse toChargeableAccountVehicleResponse(
      List<ChargeableVehicle> chargeableVehicles, String direction, int pageSize,
      boolean firstPage) {

    String firstVrn = initFirstPageVrn(chargeableVehicles, firstPage);
    String lastVrn = initLastPageVrn(chargeableVehicles, pageSize);
    String travelDirection = StringUtils.hasText(direction) ? direction : DIRECTION_NEXT;
    if (isLastPage(chargeableVehicles.size(), pageSize)) {
      // Clear First and Last VRN if the last page
      firstVrn = travelDirection.equals(DIRECTION_PREVIOUS) ? null : firstVrn;
      lastVrn = travelDirection.equals(DIRECTION_NEXT) ? null : lastVrn;
    }

    return ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(chargeableAccountVehiclesResultFrom(
            trimChargeableVehicles(chargeableVehicles, pageSize)))
        .firstVrn(firstVrn)
        .lastVrn(lastVrn)
        .build();
  }

  /**
   * Initialize First Page VRN.
   */
  private String initFirstPageVrn(List<ChargeableVehicle> chargeableVehicles, boolean firstPage) {
    if (firstPage) {
      return null;
    }

    return Iterables.getFirst(chargeableVehicles, ChargeableVehicle.builder().build()).getVrn();
  }

  /**
   * Initialize Last Page VRN.
   */
  private String initLastPageVrn(List<ChargeableVehicle> chargeableVehicles, int pageSize) {
    return Iterables.getLast(trimChargeableVehicles(chargeableVehicles, pageSize),
        ChargeableVehicle.builder().build()).getVrn();
  }

  /**
   * Checks if found vehicles are on the last page.
   */
  private boolean isLastPage(int chargeableVehiclesCount, int pageSize) {
    return chargeableVehiclesCount < pageSize + 1;
  }

  /**
   * Trim found list of ChargeableVehicles to be in a size of provided pageSize.
   */
  private List<ChargeableVehicle> trimChargeableVehicles(List<ChargeableVehicle> chargeableVehicles,
      int pageSize) {
    return chargeableVehicles.size() > pageSize ? chargeableVehicles.subList(0, pageSize)
        : chargeableVehicles;
  }

  /**
   * Converts chargeableVehicles list to {@link ChargeableAccountVehiclesResult}.
   */
  private ChargeableAccountVehiclesResult chargeableAccountVehiclesResultFrom(
      List<ChargeableVehicle> chargeableVehicles) {
    return ChargeableAccountVehiclesResult.builder()
        .results(chargeableVehicles.stream().map(
            chargeableVehicle -> VrnWithTariffAndEntrancesPaid.builder()
                .vrn(chargeableVehicle.getVrn())
                .charge(chargeableVehicle.getCharge())
                .tariffCode(chargeableVehicle.getTariffCode())
                .paidDates(chargeableVehicle.getPaidDates())
                .build())
            .collect(Collectors.toList()))
        .build();
  }

}
