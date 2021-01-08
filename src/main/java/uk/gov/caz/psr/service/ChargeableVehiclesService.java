package uk.gov.caz.psr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.ChargeableVehiclesPage;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.service.exception.ChargeableAccountVehicleNotFoundException;

/**
 * Service responsible for getting and processing chargeable vehicles from the Accounts API.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ChargeableVehiclesService {

  private final AccountService accountService;

  /**
   * Method which retrieve Chargeable Vehicles page with its paid dates.
   *
   * @param accountId selected account identifier
   * @param cazId selected Clean Air Zone ID
   * @param query part of Vehicle Registration Number for partial search
   * @param pageNumber page number
   * @param pageSize page size
   * @return list of {@link ChargeableVehicle} build based on details from accounts API
   */
  public ChargeableVehiclesPage retrieve(UUID accountId, UUID cazId, String query, int pageNumber,
      int pageSize) {
    ChargeableVehiclesPage chargeableVehiclesPage = getPageOfChargeableVehicles(accountId,
        cazId, query, pageNumber, pageSize);

    Map<String, List<EntrantPayment>> entrantPaymentsForVrns = accountService
        .getPaidEntrantPayments(getVrns(chargeableVehiclesPage.getChargeableVehicles()), cazId);

    return chargeableVehiclesPage.toBuilder().chargeableVehicles(
        chargeableVehiclesPage.getChargeableVehicles().stream()
            .map(chargeableVehicle -> chargeableVehicle.toBuilder()
                .paidDates(
                    collectPaidDatesForVrn(chargeableVehicle.getVrn(), entrantPaymentsForVrns))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Method which retrieve single Chargeable Vehicle with its paid dates.
   *
   * @param accountId selected account identifier
   * @param vrn Vehicle Registration Number to find
   * @param cazId selected Clean Air Zone ID
   * @return {@link ChargeableVehicle} build based on details from accounts API
   * @throws ChargeableAccountVehicleNotFoundException if found vehicle is not chargeable in
   *     CAZ
   */
  public ChargeableVehicle retrieveOne(UUID accountId, String vrn, UUID cazId) {
    VehicleWithCharges vehicleWithCharges = accountService
        .retrieveSingleAccountVehicle(accountId, vrn);

    if (!isVehicleChargeableInCaz(vehicleWithCharges, cazId)) {
      throw new ChargeableAccountVehicleNotFoundException();
    }

    return ChargeableVehicle.from(
        vehicleWithCharges.getVrn(),
        getCachedChargeForCaz(vehicleWithCharges, cazId),
        getPaidDatesForSingleVrn(vrn, cazId)
    );
  }

  /**
   * Gets list of chargeable vehicles from the Accounts Service.
   */
  private ChargeableVehiclesPage getPageOfChargeableVehicles(UUID accountId,
      UUID cazId, String query, int pageNumber, int pageSize) {
    VehiclesResponseDto accountVehicles = accountService
        .getAccountVehicles(accountId, pageNumber, pageSize, cazId, query);
    List<ChargeableVehicle> chargeableVehicles = accountVehicles.getVehicles().stream()
        .map(vehicleWithCharges ->
            ChargeableVehicle.from(vehicleWithCharges.getVrn(),
                getCachedChargeForCaz(vehicleWithCharges, cazId))).collect(
            Collectors.toList());

    return ChargeableVehiclesPage.builder()
        .chargeableVehicles(chargeableVehicles)
        .totalVehiclesCount(accountVehicles.getTotalVehiclesCount())
        .pageCount(accountVehicles.getPageCount())
        .build();
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
   * Gets paid Dates for provided vrn.
   */
  private List<LocalDate> getPaidDatesForSingleVrn(String vrn, UUID cazId) {
    return collectPaidDates(
        accountService.getPaidEntrantPayments(Collections.singletonList(vrn), cazId)
            .entrySet()
            .iterator()
            .next()
            .getValue()
    );
  }

  /**
   * Collect paid travel dates for provided list of {@link EntrantPayment}.
   */
  private List<LocalDate> collectPaidDates(List<EntrantPayment> entrantPayments) {
    return entrantPayments.stream()
        .map(EntrantPayment::getTravelDate)
        .collect(Collectors.toList());
  }

  /**
   * Maps list of {@link EntrantPayment} to list of its travel date.
   */
  private List<LocalDate> collectPaidDatesForVrn(String vrn,
      Map<String, List<EntrantPayment>> entrantPaymentsForVrns) {
    return collectPaidDates(entrantPaymentsForVrns.get(vrn));
  }
}
