package uk.gov.caz.psr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
