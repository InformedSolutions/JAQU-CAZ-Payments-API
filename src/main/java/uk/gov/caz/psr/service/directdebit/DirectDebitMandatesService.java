package uk.gov.caz.psr.service.directdebit;

import static java.util.stream.Collectors.groupingBy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate.DirectDebitMandateStatus;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.accounts.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.psr.dto.accounts.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.model.directdebit.Mandate;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.util.ResponseBodyUtils;

/**
 * Service that obtains information about direct debit mandates for the account.
 */
@Service
@AllArgsConstructor
@Slf4j
public class DirectDebitMandatesService {

  @VisibleForTesting
  static final EnumSet<DirectDebitMandateStatus> CACHEABLE_STATUSES = EnumSet.of(
      DirectDebitMandateStatus.ABANDONED,
      DirectDebitMandateStatus.FAILED,
      DirectDebitMandateStatus.CANCELLED,
      DirectDebitMandateStatus.INACTIVE,
      DirectDebitMandateStatus.ERROR
  );

  private static final String ERROR_BODY = ", error body: '";

  private final VccsRepository vccsRepository;
  private final AccountsRepository accountsRepository;
  private final ExternalDirectDebitRepository externalDirectDebitRepository;

  /**
   * Obtains the registered direct debit mandates for the given account by its identifier {@code
   * accountId}. If the account does not exist, a list with only clean air zones is returned.
   */
  public List<CleanAirZoneWithMandates> getDirectDebitMandates(UUID accountId) {
    try {
      log.info("Getting direct debit mandates for account '{}' : start", accountId);
      List<CleanAirZoneDto> cleanAirZones = getCleanAirZones();
      return mergeCleanAirZonesWithAssociatedMandates(cleanAirZones,
          accountId);
    } finally {
      log.info("Getting direct debit mandates for account '{}' : finish", accountId);
    }
  }

  /**
   * Obtains the registered direct debit mandates for the given account by its identifier {@code
   * accountId} and cleanAirZone {@code cleanAirZoneId}.
   */
  public List<Mandate> getMandatesForCazWithStatus(UUID accountId, UUID cleanAirZoneId) {
    try {
      log.info("Getting direct debit mandates for Account '{}' and CleanAirZone '{}' : start",
          accountId, cleanAirZoneId);

      List<DirectDebitMandate> directDebitMandates = getMandatesFromAccountsForCaz(accountId,
          cleanAirZoneId);
      return toMandates(directDebitMandates, accountId);

    } finally {
      log.info("Getting direct debit mandates for Account '{}' and CleanAirZone '{}' : finish",
          accountId, cleanAirZoneId);
    }
  }

  /**
   * Creates DirectDebitMandate in External Payment provider, store details in AccountsAPI and
   * returns nextUrl to confirm Mandate creation on the frontend.
   */
  public String createDirectDebitMandate(UUID cleanAirZoneId, UUID accountId, String returnUrl) {
    try {
      log.info("Creating direct debit mandate for account '{}' : start", accountId);
      MandateResponse externalDirectDebitMandate = externalDirectDebitRepository
          .createMandate(returnUrl, UUID.randomUUID().toString(), cleanAirZoneId);
      Response<CreateDirectDebitMandateResponse> response = accountsRepository
          .createDirectDebitMandateSync(accountId,
              CreateDirectDebitMandateRequest.builder()
                  .cleanAirZoneId(cleanAirZoneId)
                  .mandateId(externalDirectDebitMandate.getMandateId())
                  .build());
      if (!response.isSuccessful()) {
        throw new ExternalServiceCallException("Accounts service call failed, status code: "
            + response.code() + ERROR_BODY + getErrorBody(response) + "'");
      }
      return externalDirectDebitMandate.getLinks().getNextUrl().getHref();
    } finally {
      log.info("Creating direct debit mandate for account '{}' : finish", accountId);
    }
  }

  /**
   * Gets clean air zones from VCCS microservice.
   */
  private List<CleanAirZoneDto> getCleanAirZones() {
    Response<CleanAirZonesDto> response = vccsRepository.findCleanAirZonesSync();
    if (!response.isSuccessful()) {
      throw new ExternalServiceCallException("VCCS call failed, status code: "
          + response.code() + ERROR_BODY + getErrorBody(response) + "'");
    }
    return response.body().getCleanAirZones();
  }

  /**
   * Embellishes the passed clean air zones with the information about mandates, provided it
   * exists.
   */
  private List<CleanAirZoneWithMandates> mergeCleanAirZonesWithAssociatedMandates(
      List<CleanAirZoneDto> cleanAirZones, UUID accountId) {
    Map<UUID, List<DirectDebitMandate>> mandatesByCazId = getMandatesFromAccountsGroupedByCaz(
        accountId);

    return cleanAirZones.stream()
        .map(caz -> toCleanAirZoneWithMandates(mandatesByCazId, caz, accountId))
        .collect(Collectors.toList());
  }

  /**
   * Creates an instance of {@link CleanAirZoneWithMandates}.
   */
  private CleanAirZoneWithMandates toCleanAirZoneWithMandates(
      Map<UUID, List<DirectDebitMandate>> mandatesByCazId, CleanAirZoneDto caz,
      UUID accountId) {
    return CleanAirZoneWithMandates.builder()
        .cleanAirZoneId(caz.getCleanAirZoneId())
        .cazName(caz.getName())
        .mandates(toMandates(mandatesByCazId.getOrDefault(caz.getCleanAirZoneId(),
            Collections.emptyList()), accountId))
        .build();
  }

  /**
   * Gets mandates from the accounts microservice for provided CAZ id.
   */
  private List<DirectDebitMandate> getMandatesFromAccountsForCaz(UUID accountId,
      UUID cleanAirZoneId) {
    return getMandatesFromAccounts(accountId).stream()
        .filter(mandate -> mandate.getCleanAirZoneId().equals(cleanAirZoneId))
        .collect(Collectors.toList());
  }

  /**
   * Gets mandates from the accounts microservice and groups them by CAZ id.
   */
  private Map<UUID, List<DirectDebitMandate>> getMandatesFromAccountsGroupedByCaz(UUID accountId) {
    return getMandatesFromAccounts(accountId)
        .stream()
        .collect(groupingBy(DirectDebitMandate::getCleanAirZoneId));
  }

  /**
   * Gets mandates from the accounts microservice.
   */
  private List<DirectDebitMandate> getMandatesFromAccounts(UUID accountId) {
    Response<AccountDirectDebitMandatesResponse> response = accountsRepository
        .getAccountDirectDebitMandatesSync(accountId);
    if (!response.isSuccessful()) {
      throw new ExternalServiceCallException("Accounts call failed, status code: "
          + response.code() + ERROR_BODY + getErrorBody(response) + "'");
    }
    List<DirectDebitMandate> directDebitMandates = response.body().getDirectDebitMandates();
    log.info("Got {} direct debit mandates for account '{}'", directDebitMandates.size(),
        accountId);
    return directDebitMandates;
  }

  /**
   * For every mandate whose status is in {@link #CACHEABLE_STATUSES} fetches its current status
   * from GOV UK Pay service and create a new instance of {@link Mandate} with it and data from
   * {@link DirectDebitMandate}.
   */
  private List<Mandate> toMandates(List<DirectDebitMandate> mandates, UUID accountId) {
    List<MandateWithCachedAndActualStatuses> mandatesWithExternallyFetchedStatus =
        mandatesWithExternallyFetchedStatus(mandates);
    List<Mandate> mandatesWithCachedStatus = mandatesWithCachedStatus(mandates);

    updateMandateStatuses(accountId, mandatesWithExternallyFetchedStatus);

    return ImmutableList.copyOf(
        merge(mandatesWithExternallyFetchedStatus, mandatesWithCachedStatus)
    );
  }

  /**
   * Conditionally updates mandates' state in accounts service.
   */
  private void updateMandateStatuses(UUID accountId,
      List<MandateWithCachedAndActualStatuses> mandatesWithExternallyFetchedStatus) {
    Optional<DirectDebitMandatesUpdateRequest> optionalRequest = buildUpdateStatusInAccountsRequest(
        mandatesWithExternallyFetchedStatus);
    optionalRequest.ifPresent(updateRequest -> {
      Response<Void> response = accountsRepository.updateDirectDebitMandatesSync(accountId,
          updateRequest);
      log.info("Request to update statuses of {} mandates in accounts service result, "
              + "status code: {}, message: {}, error body: {}",
          updateRequest.getDirectDebitMandates().size(), response.code(), response.message(),
          getErrorBody(response));
    });
  }

  /**
   * Optionally creates an instance of {@link DirectDebitMandatesUpdateRequest} if any of the cached
   * statuses has stale value.
   */
  private Optional<DirectDebitMandatesUpdateRequest> buildUpdateStatusInAccountsRequest(
      List<MandateWithCachedAndActualStatuses> mandatesWithExternallyFetchedStatus) {
    List<SingleDirectDebitMandateUpdate> mandates = mandatesWithExternallyFetchedStatus.stream()
        .filter(mandate -> mandate.getActualStatus() != mandate.getCachedStatus())
        .map(mandate -> SingleDirectDebitMandateUpdate.builder()
            .mandateId(mandate.getPaymentProviderMandateId())
            .status(mandate.getActualStatus().name())
            .build())
        .collect(Collectors.toList());

    return mandates.isEmpty()
        ? Optional.empty()
        : Optional.of(DirectDebitMandatesUpdateRequest.builder()
            .directDebitMandates(mandates)
            .build());
  }

  /**
   * Merges provided two lists into one.
   */
  private Iterable<Mandate> merge(List<MandateWithCachedAndActualStatuses> a, List<Mandate> b) {
    return Iterables.concat(
        a.stream().map(this::toMandate).collect(Collectors.toList()),
        b
    );
  }

  /**
   * Maps {@link MandateWithCachedAndActualStatuses} to {@link Mandate}.
   */
  private Mandate toMandate(MandateWithCachedAndActualStatuses mandate) {
    return Mandate.builder()
        .id(mandate.getPaymentProviderMandateId())
        .status(mandate.getActualStatus().name())
        .build();
  }

  /**
   * Maps the provided arguments to an instance of {@link Mandate}.
   */
  private Mandate toMandate(DirectDebitMandate mandate) {
    return Mandate.builder()
        .id(mandate.getPaymentProviderMandateId())
        .status(mandate.getStatus().name()) // status cannot be null here
        .build();
  }

  /**
   * Selects only those mandates which have a cacheable status from the provided {@code mandates}
   * and maps it to {@link Mandate}.
   */
  private List<Mandate> mandatesWithCachedStatus(List<DirectDebitMandate> mandates) {
    return mandates.stream()
        .filter(DirectDebitMandatesService::shouldUseCachedStatus)
        .map(this::toMandate)
        .collect(Collectors.toList());
  }

  /**
   * Selects only those mandates which does not have a cacheable status from the provided {@code
   * mandates} and maps it to {@link Mandate}.
   */
  private List<MandateWithCachedAndActualStatuses> mandatesWithExternallyFetchedStatus(
      List<DirectDebitMandate> mandates) {
    return mandates.stream()
        .filter(DirectDebitMandatesService::shouldFetchStatusExternally)
        .map(mandate -> toMandateWithBothStatuses(mandate, fetchExternalStatusOf(mandate)))
        .collect(Collectors.toList());
  }

  /**
   * Creates an instance of {@link MandateWithCachedAndActualStatuses} based on the passed
   * arguments.
   */
  private MandateWithCachedAndActualStatuses toMandateWithBothStatuses(DirectDebitMandate mandate,
      DirectDebitMandateStatus externalStatus) {
    return MandateWithCachedAndActualStatuses.builder()
        .paymentProviderMandateId(mandate.getPaymentProviderMandateId())
        .actualStatus(externalStatus)
        .cachedStatus(mandate.getStatus())
        .build();
  }

  /**
   * Fetches the current status of a mandate from the external service.
   */
  private DirectDebitMandateStatus fetchExternalStatusOf(DirectDebitMandate mandate) {
    DirectDebitMandateStatus newStatus = DirectDebitMandateStatus.valueOf(
        externalDirectDebitRepository
            .getMandate(mandate.getPaymentProviderMandateId(), mandate.getCleanAirZoneId())
            .getState()
            .getStatus()
            .toUpperCase()
    );
    return newStatus;
  }

  /**
   * Quietly returns contents of the error body.
   */
  private <T> String getErrorBody(Response<T> response) {
    return ResponseBodyUtils.readQuietly(response.errorBody());
  }

  /**
   * Predicate that specifies whether a status for the {@code mandate} should be fetched externally
   * or not.
   */
  private static boolean shouldFetchStatusExternally(DirectDebitMandate mandate) {
    return !shouldUseCachedStatus(mandate);
  }

  /**
   * Predicate that specifies whether a status for the {@code mandate} should not be fetched
   * externally.
   */
  private static boolean shouldUseCachedStatus(DirectDebitMandate mandate) {
    return CACHEABLE_STATUSES.contains(mandate.getStatus());
  }

  /**
   * Helper value object holding two mandate statuses, the cached one and the actual one (kept
   * externally).
   */
  @Value
  @Builder
  private static class MandateWithCachedAndActualStatuses {

    @NonNull
    String paymentProviderMandateId;
    @NonNull
    DirectDebitMandateStatus actualStatus;
    DirectDebitMandateStatus cachedStatus;
  }
}
