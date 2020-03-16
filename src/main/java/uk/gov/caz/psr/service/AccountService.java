package uk.gov.caz.psr.service;

import com.amazonaws.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.AccountVehicleResponse;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.AccountNotFoundException;
import uk.gov.caz.psr.service.exception.ChargeableAccountVehicleNotFoundException;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

/**
 * Service to interact with the Accounts Service.
 */
@AllArgsConstructor
@Service
public class AccountService {

  private final AccountsRepository accountsRepository;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;
  private final VccsRepository vccRepository;

  /**
   * Retrieve a page of VRNs of vehicles associated with a given account ID.
   *
   * @param accountId the id of the account
   * @param pageNumber the number of the page
   * @param pageSize the size of the page
   */
  public AccountVehicleRetrievalResponse retrieveAccountVehicles(UUID accountId,
      String pageNumber, String pageSize) {
    Response<AccountVehicleRetrievalResponse> accountsResponse = accountsRepository
        .getAccountVehicleVrnsSync(accountId, pageNumber, pageSize);
    if (accountsResponse.isSuccessful()) {
      return accountsResponse.body();
    } else {
      switch (accountsResponse.code()) {
        case 404:
          throw new AccountNotFoundException();
        default:
          throw new ExternalServiceCallException();
      }
    }
  }

  /**
   * Fetches a list of vehicles from the Accounts Service and lazily checks their chargeability
   * until it generates a full list of results.
   *
   * @param accountId the account whose vehicles should be returned
   * @param direction 'next' or 'previous' in terms of pages
   * @param pageSize the size of the list to be returned
   * @param vrn the "cursor" on which to search the account vehicles
   * @param cleanAirZoneId the Clean Air Zone to check compliance for
   * @return a list of chargeable VRNs
   */
  public List<VrnWithTariffAndEntrancesPaid> retrieveChargeableAccountVehicles(UUID accountId,
      String direction, int pageSize, String vrn, UUID cleanAirZoneId) {
    List<VrnWithTariffAndEntrancesPaid> results = new ArrayList<VrnWithTariffAndEntrancesPaid>();
    Boolean lastPage = false;
    // initialise cursor at first VRN
    String vrnCursor = vrn;

    while (results.size() < (pageSize + 1) && !lastPage) {
      // get triple the number of vrns as will be on page to reduce overall request numbers
      List<String> accountVrns = getAccountVrns(accountId, direction, pageSize * 3, vrnCursor);

      // check if the end of pages has been reached, if not set new cursor
      if (accountVrns.size() < pageSize * 3) {
        lastPage = true;
      } else {
        vrnCursor = getVrnCursor(accountVrns, direction);
      }

      List<VrnWithTariffAndEntrancesPaid> chargeableVrns =
          getChargeableVrnsFromVcc(accountVrns, cleanAirZoneId, pageSize);

      if (!chargeableVrns.isEmpty()) {
        results.addAll(chargeableVrns);
      }
    }

    return results;
  }

  /**
   * Method for retrieving a single chargeable vehicle linked to an account by a quoted vrn.
   *
   * @param accountId the unique id of the user account
   * @param vrn the vrn to query for chargeability
   * @param cleanAirZoneId the clean air zone to check chargeability against
   * @return a list of chargeable VRNs
   */
  public ChargeableAccountVehiclesResult retrieveSingleChargeableAccountVehicle(
      UUID accountId, String vrn, UUID cleanAirZoneId) {

    Response<AccountVehicleResponse> accountVehicle =
        accountsRepository.getAccountSingleVehicleVrnSync(accountId, vrn);

    // If vehicle could not be found, yield early 404.
    if (accountVehicle.code() == HttpStatus.NOT_FOUND.value()) {
      throw new ChargeableAccountVehicleNotFoundException();
    }

    List<String> wrappedVrn = Arrays.asList(vrn);
    List<VrnWithTariffAndEntrancesPaid> vrnsWithTariff =
        getChargeableVrnsFromVcc(wrappedVrn, cleanAirZoneId, 1);
    List<String> chargeableVrns = vrnsWithTariff.stream()
        .map(vrnWithTariff -> vrnWithTariff.getVrn())
        .collect(Collectors.toList());

    return ChargeableAccountVehiclesResult
        .from(getPaidEntrantPayments(chargeableVrns, cleanAirZoneId), vrnsWithTariff);
  }

  /**
   * Gets entrant payments for a list of VRNs in a 13 day payment window.
   *
   * @param results map of VRNs against entrant payments
   * @param cleanAirZoneId an identitier for the clean air zone
   */
  public Map<String, List<EntrantPayment>> getPaidEntrantPayments(
      List<String> results, UUID cleanAirZoneId) {
    return getPaidEntrantPaymentsService.getResults(
        new HashSet<>(results),
        LocalDate.now().minusDays(6),
        LocalDate.now().plusDays(6),
        cleanAirZoneId);
  }

  private List<VrnWithTariffAndEntrancesPaid> getChargeableVrnsFromVcc(List<String> accountVrns,
      UUID cleanAirZoneId, int pageSize) {
    List<VrnWithTariffAndEntrancesPaid> results = new ArrayList<VrnWithTariffAndEntrancesPaid>();
    // split accountVrns into chunks
    List<List<String>> accountVrnChunks = Lists.partition(accountVrns, pageSize);

    // while results is less than page size do another batch
    for (List<String> chunk : accountVrnChunks) {
      List<ComplianceResultsDto> complianceOutcomes = vehicleComplianceRetrievalService
          .retrieveVehicleCompliance(chunk, cleanAirZoneId.toString());
      List<VrnWithTariffAndEntrancesPaid> chargeableVrns = complianceOutcomes
          .stream()
          .filter(complianceOutcome -> vrnIsChargeable(complianceOutcome))
          .map(complianceOutcome -> ChargeableAccountVehiclesResult
              .buildVrnWithTariffAndEntrancesPaidFrom(complianceOutcome, cleanAirZoneId))
          .collect(Collectors.toList());

      if (!chargeableVrns.isEmpty()) {
        results.addAll(chargeableVrns);
      }

      if (results.size() >= pageSize + 1) {
        break;
      }
    }

    return results;
  }

  private List<String> getAccountVrns(UUID accountId, String direction, int pageSize,
      String vrnCursor) {
    Response<List<String>> accountsResponse = accountsRepository
        .getAccountVehicleVrnsByCursorSync(accountId, direction, pageSize, vrnCursor);
    return accountsResponse.body();
  }

  private String getVrnCursor(List<String> accountVrns, String direction) {
    Collections.sort(accountVrns);
    // assume next is direction is null or empty
    if (StringUtils.isNullOrEmpty(direction) || direction.equals("next")) {
      return accountVrns.get(accountVrns.size() - 1);
    }

    if (direction.equals("previous")) {
      return accountVrns.get(0);
    }

    throw new IllegalArgumentException("Direction given is invalid.");
  }

  private Boolean vrnIsChargeable(ComplianceResultsDto complianceOutcome) {
    Preconditions.checkArgument(complianceOutcome.getComplianceOutcomes().size() <= 1);
    if (complianceOutcome.getComplianceOutcomes().isEmpty()) {
      return false;
    } else {
      float charge = complianceOutcome.getComplianceOutcomes().get(0).getCharge();
      return charge > 0;
    }
  }

  /**
   * Helper method for retrieving a list of comma delimited clean air zones IDs.
   *
   * @return a list of comma delimited clean air zones IDs.
   */
  public String getZonesQueryStringEquivalent() {
    Response<CleanAirZonesResponse> zones = vccRepository.findCleanAirZonesSync();
    List<CleanAirZoneDto> cleanAirZoneDtos = zones.body().getCleanAirZones();
    List<String> mappedZoneIds = new ArrayList<String>();

    for (CleanAirZoneDto dto : cleanAirZoneDtos) {
      mappedZoneIds.add(dto.getCleanAirZoneId().toString());
    }

    return String.join(",", mappedZoneIds);
  }

}