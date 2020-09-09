package uk.gov.caz.psr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.EntrantPayment;

/**
 * Service responsible for getting and processing chargeable vehicles from the Accounts API.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ChargeableVehiclesService {

  private static final String DIRECTION_PREVIOUS = "previous";
  private static final String DIRECTION_NEXT = "next";

  private final AccountService accountService;

  /**
   * Method which retrieve Chargeable Vehicles with its paid dates with cursor pagination.
   *
   * @param accountId selected account identifier
   * @param cursorVrn Vehicle Registration Number for cursor pagination
   * @param cazId selected Clean Air Zone ID
   * @param direction direction of pagination
   * @param pageSize page size
   * @return list of {@link ChargeableVehicle} build based on details from accounts API
   */
  public List<ChargeableVehicle> retrieve(UUID accountId, String cursorVrn, UUID cazId,
      String direction, int pageSize) {
    checkDirection(direction, cursorVrn);

    List<ChargeableVehicle> chargeableVehicles = getPageOfChargeableVehicles(accountId, cursorVrn,
        cazId, direction, pageSize);

    Map<String, List<EntrantPayment>> entrantPaymentsForVrns = accountService
        .getPaidEntrantPayments(getVrns(chargeableVehicles), cazId);

    return chargeableVehicles.stream()
        .map(chargeableVehicle -> chargeableVehicle.toBuilder()
            .paidDates(collectPaidDatesForVrn(chargeableVehicle.getVrn(), entrantPaymentsForVrns))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Check and validates direction of pagination.
   */
  private void checkDirection(String direction, String vrn) {
    if (StringUtils.hasText(direction) && !direction.equals(DIRECTION_NEXT)
        && !direction.equals(DIRECTION_PREVIOUS)) {
      throw new InvalidRequestPayloadException(
          "Direction supplied must be one of either 'next' or 'previous'.");
    }

    if (StringUtils.hasText(direction) && direction.equals(DIRECTION_PREVIOUS)
        && !StringUtils.hasText(vrn)) {
      throw new InvalidRequestPayloadException(
          "Direction cannot be set to 'previous' if no VRN has been provided.");
    }
  }

  /**
   * Gets list of chargeable vehicles from the Accounts Service.
   */
  private List<ChargeableVehicle> getPageOfChargeableVehicles(UUID accountId, String vrn,
      UUID cazId, String direction,
      int pageSize) {
    Boolean lastPage = false;
    String cursorVrn = vrn;
    List<ChargeableVehicle> results = new ArrayList<>();

    while (results.size() < (pageSize + 1) && !lastPage) {
      log.info("Fetching page of vehicles form Accounts API");
      ChargeableVehiclesResponseDto accountVehicles = accountService
          .getAccountVehiclesByCursor(accountId, direction, pageSize * 3, cursorVrn);

      List<ChargeableVehicle> foundChargeableVehicle = accountVehicles.getVehicles().stream()
          .filter(vehicle -> isVehicleChargeableInCaz(vehicle, cazId))
          .map(vehicle -> ChargeableVehicle
              .from(vehicle.getVrn(), getCachedChargeForCaz(vehicle, cazId))).collect(
              Collectors.toList());
      results.addAll(foundChargeableVehicle);

      // check if the end of pages has been reached, if not set new cursor
      if (accountVehicles.getVehicles().size() < pageSize * 3) {
        lastPage = true;
      } else {
        cursorVrn = accountVehicles.getVehicles().get(accountVehicles.getVehicles().size() - 1)
            .getVrn();
      }
    }

    return results;
  }

  /**
   * checks if vehicle is chargeable.
   */
  private boolean isVehicleChargeableInCaz(VehicleWithCharges vehicleWithCharges, UUID cazId) {
    Optional<VehicleCharge> vehicleCharge = vehicleWithCharges.getCachedCharges().stream()
        .filter(cachedCharge -> cachedCharge.getCazId().equals(cazId))
        .findFirst();
    return vehicleCharge.isPresent() && vehicleCharge.get().getCharge() != null
        && vehicleCharge.get().getCharge().compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Gets Chargeable vrns from API response.
   */
  private List<String> getVrns(List<ChargeableVehicle> chargeableVehicles) {
    return chargeableVehicles.stream()
        .map(ChargeableVehicle::getVrn)
        .collect(Collectors.toList());
  }

  /**
   * Gets cachedCharge from Api response for selected Clean Air Zone.
   */
  private VehicleCharge getCachedChargeForCaz(VehicleWithCharges chargeableAccountVehicle,
      UUID cazId) {
    return chargeableAccountVehicle.getCachedCharges().stream()
        .filter(cachedCharge -> cachedCharge.getCazId().equals(cazId))
        .iterator().next();
  }

  /**
   * Maps list of {@link EntrantPayment} to list of its travel date.
   */
  private static List<LocalDate> collectPaidDatesForVrn(String vrn,
      Map<String, List<EntrantPayment>> entrantPaymentsForVrns) {
    return entrantPaymentsForVrns.get(vrn).stream()
        .map(EntrantPayment::getTravelDate)
        .collect(Collectors.toList());
  }
}
