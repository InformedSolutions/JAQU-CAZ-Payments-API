package uk.gov.caz.psr.service;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.psr.dto.AccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult.VrnWithTariffAndEntrancesPaid;
import uk.gov.caz.psr.dto.accounts.UserDetailsResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.AccountNotFoundException;
import uk.gov.caz.psr.service.exception.ChargeableAccountVehicleNotFoundException;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.service.exception.UserNotFoundException;

/**
 * Service to interact with the Accounts Service.
 */
@AllArgsConstructor
@Service
public class AccountService {

  private static final String ADMINISTRATOR = "Administrator";
  private static final String DELETED_USER = "Deleted user";
  private final AccountsRepository accountsRepository;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

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

  /**
   * Fetches a list of vehicles from the Accounts Service.
   *
   * @param accountId the account whose vehicles should be returned
   * @param direction 'next' or 'previous' in terms of pages
   * @param pageSize the size of the list to be returned
   * @param vrnCursor the "cursor" on which to search the account vehicles
   * @return a response from the Accounts API
   * @throws AccountNotFoundException if API returns status 404
   */
  public ChargeableVehiclesResponseDto getAccountVehiclesByCursor(UUID accountId,
      String direction, int pageSize, String vrnCursor) {
    Response<ChargeableVehiclesResponseDto> accountsResponse = accountsRepository
        .getAccountChargeableVehiclesByCursorSync(accountId, direction, pageSize, vrnCursor);
    if (accountsResponse.isSuccessful()) {
      return accountsResponse.body();
    } else {
      if (accountsResponse.code() == 404) {
        throw new AccountNotFoundException();
      } else {
        throw new ExternalServiceCallException();
      }
    }
  }

  /**
   * Method for retrieving a payer name.
   *
   * @param userId {@link UUID}
   * @return {@link String} When a given user is an account owner, payerName should be
   *     “Administrator”. If a user has been deleted it should be set to “Deleted user” otherwise
   *     should return name.
   */
  public String getPayerName(UUID userId) {
    UserDetailsResponse userDetails = getUserDetails(userId);
    if (userDetails.isOwner()) {
      return ADMINISTRATOR;
    }
    if (userDetails.isRemoved()) {
      return DELETED_USER;
    }
    return userDetails.getName();
  }

  private UserDetailsResponse getUserDetails(UUID userId) {
    Response<UserDetailsResponse> userDetailsResponse = accountsRepository
        .getUserDetailsSync(userId);
    if (userDetailsResponse.isSuccessful()) {
      return userDetailsResponse.body();
    } else {
      if (userDetailsResponse.code() == 404) {
        throw new UserNotFoundException();
      } else {
        throw new ExternalServiceCallException();
      }
    }
  }

  private Boolean vrnIsChargeable(ComplianceResultsDto complianceOutcome) {
    if (complianceOutcome.getComplianceOutcomes().isEmpty()) {
      return false;
    } else {
      return complianceOutcome.getComplianceOutcomes().get(0).getCharge() > 0;
    }
  }

  private List<VrnWithTariffAndEntrancesPaid> getChargeableVrnsFromVcc(List<String> accountVrns,
      UUID cleanAirZoneId, int pageSize) {
    List<VrnWithTariffAndEntrancesPaid> results = new ArrayList<>();
    // split accountVrns into chunks
    List<List<String>> accountVrnChunks = Lists.partition(accountVrns, pageSize);

    // while results is less than page size do another batch
    for (List<String> chunk : accountVrnChunks) {
      List<ComplianceResultsDto> complianceOutcomes = vehicleComplianceRetrievalService
          .retrieveVehicleCompliance(chunk, cleanAirZoneId.toString());
      List<VrnWithTariffAndEntrancesPaid> chargeableVrns = complianceOutcomes
          .stream()
          .filter(this::vrnIsChargeable)
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
}