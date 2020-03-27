package uk.gov.caz.psr.service.directdebit;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateRequest;
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
   * Creates DirectDebitMandate in External Payment provider, store details in AccountsAPI and
   * returns nextUrl to confirm Mandate creation on the frontend.
   */
  public String createDirectDebitMandate(UUID cleanAirZoneId, UUID accountId, String returnUrl) {
    try {
      log.info("Creating direct debit mandate for account '{}' : start", accountId);
      MandateResponse externalDirectDebitMandate = externalDirectDebitRepository
          .createMandate(returnUrl, UUID.randomUUID().toString(), cleanAirZoneId);
      accountsRepository.createDirectDebitMandateSync(accountId,
          CreateDirectDebitMandateRequest.builder()
              .cleanAirZoneId(cleanAirZoneId)
              .mandateId(externalDirectDebitMandate.getMandateId())
              .build());
      return externalDirectDebitMandate.getLinks().getNextUrl().getHref();
    } finally {
      log.info("Creating direct debit mandate for account '{}' : finish", accountId);
    }
  }

  /**
   * Gets clean air zones from VCCS microservice.
   */
  private List<CleanAirZoneDto> getCleanAirZones() {
    Response<CleanAirZonesResponse> response = vccsRepository.findCleanAirZonesSync();
    if (!response.isSuccessful()) {
      throw new ExternalServiceCallException("VCCS call failed, status code: "
          + response.code() + ", error body: '" + getErrorBody(response) + "'");
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
        .map(caz -> toCleanAirZoneWithMandates(mandatesByCazId, caz))
        .collect(Collectors.toList());
  }

  /**
   * Creates an instance of {@link CleanAirZoneWithMandates} with the current status of a mandate
   * obtained externally.
   */
  private CleanAirZoneWithMandates toCleanAirZoneWithMandates(
      Map<UUID, List<DirectDebitMandate>> mandatesByCazId, CleanAirZoneDto caz) {
    return CleanAirZoneWithMandates.builder()
        .cleanAirZoneId(caz.getCleanAirZoneId())
        .cazName(caz.getName())
        .mandates(
            updateStatusOf(mandatesByCazId.getOrDefault(caz.getCleanAirZoneId(),
                Collections.emptyList()))
        )
        .build();
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
          + response.code() + ", error body: '" + getErrorBody(response) + "'");
    }
    List<DirectDebitMandate> directDebitMandates = response.body().getDirectDebitMandates();
    log.info("Got {} direct debit mandates for account '{}'", directDebitMandates.size(),
        accountId);
    return directDebitMandates;
  }

  /**
   * For every mandate in the {@code mandates} fetches its actual status in GOV UK Pay service and
   * create a new instance of {@link Mandate} with it and data from {@link DirectDebitMandate}.
   */
  private List<Mandate> updateStatusOf(List<DirectDebitMandate> mandates) {
    return mandates.stream()
        .map(mandate -> {
          MandateResponse externalMandate = externalDirectDebitRepository
              .getMandate(mandate.getPaymentProviderMandateId(),
                  mandate.getCleanAirZoneId());
          return Mandate.builder()
              .id(mandate.getPaymentProviderMandateId())
              .reference(externalMandate.getReference())
              .status(externalMandate.getState().getStatus())
              .build();
        })
        .collect(Collectors.toList());
  }

  /**
   * Quietly returns contents of the error body.
   */
  private <T> String getErrorBody(Response<T> response) {
    return ResponseBodyUtils.readQuietly(response.errorBody());
  }
}
