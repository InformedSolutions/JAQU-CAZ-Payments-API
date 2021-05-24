package uk.gov.caz.psr.service.generatecsv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.accounts.AccountUserResponse;
import uk.gov.caz.psr.dto.accounts.AccountUsersResponse;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.model.generatecsv.CsvEntrantPayment;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment.EnrichedCsvEntrantPaymentBuilder;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.generatecsv.CsvEntrantPaymentRepository;

/**
 * Generates content for csv file.
 */
@Component
@AllArgsConstructor
public class CsvContentGenerator {

  private static final String ADMINISTRATOR = "Administrator";
  private static final String DELETED_USER = "Deleted user";

  private final AccountsRepository accountsRepository;
  private final CsvEntrantPaymentRepository csvEntrantPaymentRepository;
  private final PaymentDetailRepository paymentDetailRepository;
  private final VccsRepository vccsRepository;
  private final CsvContentGeneratorStrategyFactory csvContentGeneratorStrategyFactory;

  /**
   * Generate {@link List} of String[] which contains csv rows.
   *
   * @param accountId ID of Account.
   * @param accountUserIds List of account user ids for which we should generate payment
   *     history.
   * @return {@link List} of String[] which contains csv rows.
   */
  public List<String[]> generateCsvRows(UUID accountId, List<UUID> accountUserIds) {
    List<String[]> csvRows = new ArrayList<>();
    List<AccountUserResponse> accountUsers = getAccountUsers(accountId);
    List<CsvEntrantPayment> entrantPayments = csvEntrantPaymentRepository.findAllForAccountUsers(
        accountUserIds);
    List<EnrichedCsvEntrantPayment> enrichedEntrantPayments = enrichEntrantPayments(
        entrantPayments, accountUsers);

    CsvContentGeneratorStrategy strategy = csvContentGeneratorStrategyFactory
        .createStrategy(enrichedEntrantPayments);

    csvRows.add(strategy.generateCsvHeader());
    csvRows.addAll(strategy.generateCsvContent(enrichedEntrantPayments));

    return csvRows;
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
   * Method which enrich provided {@code CsvEntrantPayment} list with data from External APIs and
   * collect it as {@code EnrichedCsvEntrantPayment}.
   */
  private List<EnrichedCsvEntrantPayment> enrichEntrantPayments(
      List<CsvEntrantPayment> entrantPayments, List<AccountUserResponse> accountUsers) {

    Response<CleanAirZonesDto> cleanAirZonesResponse = vccsRepository.findCleanAirZonesSync();
    List<UUID> paymentIds = getPaymentIds(entrantPayments);
    List<PaymentModification> paymentModifications = paymentDetailRepository
        .findAllForPaymentsHistory(paymentIds, EntrantPaymentUpdateActor.LA,
            Arrays.asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK));
    return entrantPayments.stream()
        .map(entrantPayment -> enrichEntrantPayment(entrantPayment, accountUsers,
            cleanAirZonesResponse, paymentModifications))
        .collect(Collectors.toList());
  }

  /**
   * Method which enrich provided {@code CsvEntrantPayment} with data from External APIs and returns
   * it as {@code EnrichedCsvEntrantPayment}.
   */
  private EnrichedCsvEntrantPayment enrichEntrantPayment(CsvEntrantPayment entrantPayment,
      List<AccountUserResponse> accountUsers, Response<CleanAirZonesDto> cleanAirZones,
      List<PaymentModification> paymentModifications) {
    Optional<PaymentModification> paymentModification = getModification(paymentModifications,
        entrantPayment);

    EnrichedCsvEntrantPaymentBuilder enrichedCsvEntrantPayment = EnrichedCsvEntrantPayment.builder()
        .paymentId(entrantPayment.getPaymentId())
        .dateOfPayment(entrantPayment.getDateOfPayment().toLocalDate())
        .paymentMadeBy(getPayerName(entrantPayment.getUserId(), accountUsers))
        .cazName(getCleanAirZoneName(cleanAirZones, entrantPayment.getCleanAirZoneId()))
        .vrn(entrantPayment.getVrn())
        .dateOfEntry(entrantPayment.getTravelDate())
        .charge(entrantPayment.getCharge())
        .paymentReference(entrantPayment.getPaymentReference())
        .paymentProviderId(entrantPayment.getPaymentProviderId())
        .entriesCount(entrantPayment.getEntriesCount())
        .totalPaid(entrantPayment.getTotalPaid());

    paymentModification.ifPresent(modification -> enrichedCsvEntrantPayment
        .status(modification.getEntrantPaymentStatus())
        .dateReceivedFromLa(modification.getModificationTimestamp().toLocalDate())
        .caseReference(modification.getCaseReference()));
    return enrichedCsvEntrantPayment.build();
  }

  /**
   * Method for retrieving a payer name.
   */
  private String getPayerName(UUID userId, List<AccountUserResponse> accountUsers) {
    AccountUserResponse accountUser = accountUsers.stream()
        .filter(accountUserResponse -> accountUserResponse.getAccountUserId().equals(userId))
        .iterator().next();
    if (accountUser.isOwner()) {
      return ADMINISTRATOR;
    }
    if (accountUser.isRemoved()) {
      return DELETED_USER;
    }
    return accountUser.getName();
  }

  /**
   * Method for retrieving a CleanAirZOne name.
   */
  private String getCleanAirZoneName(Response<CleanAirZonesDto> cleanAirZonesResponse,
      UUID cleanAirZone) {

    return cleanAirZonesResponse.body().getCleanAirZones().stream()
        .filter(caz -> caz.getCleanAirZoneId().equals(cleanAirZone))
        .map(CleanAirZoneDto::getName)
        .findFirst()
        .orElse(null);
  }

  /**
   * Get list of PaymentIds based on entrant Payments.
   */
  private List<UUID> getPaymentIds(List<CsvEntrantPayment> entrantPayments) {
    return entrantPayments.stream()
        .map(CsvEntrantPayment::getPaymentId)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Gets modification status for selected EntrantPayment.
   */
  private Optional<PaymentModification> getModification(
      List<PaymentModification> paymentModifications, CsvEntrantPayment entrantPayment) {
    return paymentModifications.stream()
        .filter(paymentModification -> paymentModification.getVrn().equals(entrantPayment.getVrn())
            && paymentModification.getTravelDate().equals(entrantPayment.getTravelDate())
            && paymentModification.getPaymentId().equals(entrantPayment.getPaymentId()))
        .reduce((first, second) -> second); // get last element
  }
}
