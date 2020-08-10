package uk.gov.caz.psr.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.psr.dto.accounts.AccountUserResponse;
import uk.gov.caz.psr.dto.accounts.AccountUsersResponse;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;
import uk.gov.caz.psr.model.PaymentSummary;
import uk.gov.caz.psr.model.PaymentToCleanAirZoneMapping;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.PaymentSummaryRepository;
import uk.gov.caz.psr.repository.PaymentToCleanAirZoneMappingRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Service responsible for fetching users information from AccountsAPI, fetching information about
 * payments of those users from the database, getting information about CleanAirZones from VCCS API,
 * and finally, integration of those data.
 */
@Service
@AllArgsConstructor
public class RetrieveSuccessfulPaymentsService {

  private final AccountsRepository accountsRepository;
  private final VccsRepository vccsRepository;
  private final PaymentToCleanAirZoneMappingRepository paymentToCleanAirZoneMappingRepository;
  private final PaymentSummaryRepository paymentSummaryRepository;
  private final CurrencyFormatter currencyFormatter;
  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

  /**
   * Method fetches list of users associated with the provided account and selects the user with the
   * provided accountId.
   *
   * @param accountId Account of which users are going to be fetched
   * @param accountUserId Specific account which data are going to be fetched
   * @param pageNumber page number
   * @param pageSize page size
   */
  public Pair<PaginationData, List<EnrichedPaymentSummary>> retrieveForSingleUser(UUID accountId,
      UUID accountUserId, int pageNumber, int pageSize) {
    List<AccountUserResponse> accountUsers = getAccountUsers(accountId);
    List<AccountUserResponse> selectedAccountUser = accountUsers.stream()
        .filter(accountUserResponse -> accountUserResponse.getAccountUserId().equals(accountUserId))
        .collect(Collectors.toList());

    return retrieveForSelectedUsers(selectedAccountUser, pageNumber, pageSize);
  }

  /**
   * Method fetches list of users associated with the provided account and asks for data of all
   * users associated with the account.
   *
   * @param accountId Account of which users are going to be fetched
   * @param pageNumber page number
   * @param pageSize page size
   */
  public Pair<PaginationData, List<EnrichedPaymentSummary>> retrieveForAccount(UUID accountId,
      int pageNumber, int pageSize) {
    List<AccountUserResponse> accountUsers = getAccountUsers(accountId);
    return retrieveForSelectedUsers(accountUsers, pageNumber, pageSize);
  }

  /**
   * Method receives list of users for which the payment data are going to be fetched from the
   * database.
   *
   * @param accountUsers list of users
   * @param pageNumber page number
   * @param pageSize page size
   */
  private Pair<PaginationData, List<EnrichedPaymentSummary>> retrieveForSelectedUsers(
      List<AccountUserResponse> accountUsers, int pageNumber, int pageSize) {
    List<UUID> userIds = getUserIds(accountUsers);
    Map<UUID, String> accountIdToAccountNameMap = getAccountIdToAccountNameMap(accountUsers);

    List<PaymentSummary> paymentSummaries = composePaymentSummaries(userIds, pageNumber, pageSize);
    List<EnrichedPaymentSummary> enrichedPaymentSummaries = enrichPaymentSummaries(
        paymentSummaries, accountIdToAccountNameMap);
    PaginationData paginationData = getPaginationData(userIds, pageNumber, pageSize);

    return Pair.of(paginationData, enrichedPaymentSummaries);
  }

  /**
   * Method receives userIds list whose payments are going to be fetched from DB, and composes it
   * with clean air zone ids.
   *
   * @param userIds list of userIds
   * @param pageNumber page number
   * @param pageSize page size
   * @return list of {@link PaymentSummary}.
   */
  private List<PaymentSummary> composePaymentSummaries(List<UUID> userIds, int pageNumber,
      int pageSize) {
    Map<UUID, UUID> paymentIdToCazIdMap = getPaymentIdToCleanAirZoneIdMap(userIds);

    List<PaymentSummary> paymentSummaries = paymentSummaryRepository
        .getPaginatedPaymentSummaryForUserIds(userIds, pageNumber, pageSize);

    return paymentSummaries
        .stream()
        .map(paymentSummary -> paymentSummary.toBuilder()
            .cleanAirZoneId(paymentIdToCazIdMap.get(paymentSummary.getPaymentId()))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Method receives collection of {@link AccountUserResponse} and returns list only with their
   * IDs.
   *
   * @param accountUsers list of {@link AccountUserResponse}
   * @return list of user Ids.
   */
  private List<UUID> getUserIds(List<AccountUserResponse> accountUsers) {
    return accountUsers.stream().map(user -> user.getAccountUserId()).collect(
        Collectors.toList());
  }

  /**
   * Method returns helper collection which stores mapping between userId and it's name (e.g.
   * 'bf0155a9-d7cf-4365-afea-621a3af48b16' => 'John Doe').
   *
   * @param accountUsers collection of {@link AccountUserResponse}
   * @return helper collection.
   */
  private Map<UUID, String> getAccountIdToAccountNameMap(List<AccountUserResponse> accountUsers) {
    return accountUsers.stream().collect(
        Collectors.toMap(AccountUserResponse::getAccountUserId, this::selectNameForAccountUser));
  }

  /**
   * Method performs an API call to the accounts service in order to get users information.
   *
   * @param accountId ID of the account of which users are going to be fetched.
   * @return list of fetched users.
   */
  private List<AccountUserResponse> getAccountUsers(UUID accountId) {
    Response<AccountUsersResponse> accountUsersBody = accountsRepository.getAllUsersSync(accountId);
    return accountUsersBody.body().getUsers();
  }

  /**
   * Based on the provided users ids, the counting queries are triggered on the database in order to
   * return pagination information.
   *
   * @param userIds list of user ids
   * @param pageNumber page number
   * @param pageSize page size
   * @return {@link PaginationData} object.
   */
  private PaginationData getPaginationData(List<UUID> userIds, int pageNumber, int pageSize) {
    int totalPaymentsCount = paymentSummaryRepository.getTotalPaymentsCountForUserIds(userIds);
    int pageCount = totalPaymentsCount / pageSize + ((totalPaymentsCount % pageSize == 0) ? 0 : 1);

    return PaginationData.builder()
        .pageSize(pageSize)
        .pageNumber(pageNumber)
        .totalElementsCount(totalPaymentsCount)
        .pageCount(pageCount)
        .build();
  }

  /**
   * Helper method which queries the database for the paymentId and cleanAirZone association and
   * then returns map with paymentId as key and cleanAirZoneId as value (e.g.
   * '9cc2dd1a-905e-4eaf-af85-0b14f95aab89' => '43ea77cc-93cb-4df3-b731-5244c0de9cc8').
   *
   * @param userIds collection of userIds for whose payments database is queried.
   * @return map
   */
  private Map<UUID, UUID> getPaymentIdToCleanAirZoneIdMap(List<UUID> userIds) {
    List<PaymentToCleanAirZoneMapping> mappings = paymentToCleanAirZoneMappingRepository
        .getPaymentToCleanAirZoneMapping(userIds);
    Map<UUID, UUID> paymentToCleanAirZoneMap = mappings.stream().collect(Collectors
        .toMap(PaymentToCleanAirZoneMapping::getPaymentId,
            PaymentToCleanAirZoneMapping::getCleanAirZoneId));

    return paymentToCleanAirZoneMap;
  }

  /**
   * Method receives {@link PaymentSummary} collection along with helper collection which has stored
   * mapping between cleanAirZoneID and cleanAirZone name.
   *
   * @param paymentSummaries collection of {@link PaymentSummary}
   * @param accountIdToNameMap helper collection
   * @return collection of {@link EnrichedPaymentSummary} with all data.
   */
  private List<EnrichedPaymentSummary> enrichPaymentSummaries(
      List<PaymentSummary> paymentSummaries, Map<UUID, String> accountIdToNameMap) {
    Map<UUID, String> cleanAirZonesIdToNameMap = vehicleComplianceRetrievalService
        .getCleanAirZoneIdToCleanAirZoneNameMap();

    List<EnrichedPaymentSummary> enrichedPaymentSummaries = paymentSummaries.stream()
        .map(paymentSummary -> EnrichedPaymentSummary.builder()
            .paymentId(paymentSummary.getPaymentId())
            .entriesCount(paymentSummary.getEntriesCount())
            .totalPaid(currencyFormatter.parsePenniesToBigDecimal(paymentSummary.getTotalPaid()))
            .cazName(cleanAirZonesIdToNameMap.get(paymentSummary.getCleanAirZoneId()))
            .payerName(accountIdToNameMap.get(paymentSummary.getPayerId()))
            .paymentDate(paymentSummary.getPaymentDate())
            .build()).collect(Collectors.toList());

    return enrichedPaymentSummaries;
  }



  /**
   * Helper method which returns proper payerName based on the provided {@link
   * AccountUserResponse}.
   *
   * @param accountUser {@link AccountUserResponse} object.
   * @return String with payerName.
   */
  private String selectNameForAccountUser(AccountUserResponse accountUser) {
    if (accountUser.isOwner()) {
      return "Administrator";
    }
    if (accountUser.isRemoved()) {
      return "Deleted user";
    }
    return accountUser.getName();
  }
}
