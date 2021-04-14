package uk.gov.caz.psr.service.generatecsv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * Generates content for csv file.
 */
@Component
@AllArgsConstructor
public class CsvContentGenerator {

  private static final String CSV_HEADER = "Date of payment,Payment made by,"
      + "Clean Air Zone,Number plate,Date of entry,Charge,Payment reference,"
      + "GOV.UK payment ID,Entries paid for,Total amount paid,Status,"
      + "Date received from local authority,Case reference";
  private static final String ADMINISTRATOR = "Administrator";
  private static final String DELETED_USER = "Deleted user";

  private final AccountsRepository accountsRepository;
  private final CsvEntrantPaymentRepository csvEntrantPaymentRepository;
  private final CurrencyFormatter currencyFormatter;
  private final PaymentDetailRepository paymentDetailRepository;
  private final VccsRepository vccsRepository;

  /**
   * Generate {@link List} of String[] which contains csv rows.
   *
   * @param accountId ID of Account.
   * @param accountUserId ID of AccountUser.
   * @return {@link List} of String[] which contains csv rows.
   */
  public List<String[]> generateCsvRows(UUID accountId, UUID accountUserId) {
    List<String[]> csvRows = new ArrayList<>();

    List<AccountUserResponse> accountUsers = getAccountUsers(accountId);
    List<UUID> selectedAccountUserIds = selectAccountUsers(accountUsers, accountUserId);
    List<CsvEntrantPayment> entrantPayments = csvEntrantPaymentRepository.findAllForAccountUsers(
        selectedAccountUserIds);
    List<EnrichedCsvEntrantPayment> enrichedEntrantPayments = enrichEntrantPayments(
        entrantPayments, accountUsers);

    // adding header record
    csvRows.add(new String[]{CSV_HEADER});

    for (EnrichedCsvEntrantPayment enrichedCsvEntrantPayment : enrichedEntrantPayments) {
      csvRows.add(new String[]{createCsvRow(enrichedCsvEntrantPayment)});
    }
    return csvRows;
  }

  /**
   * Returns list of accountUserIDs for selected accountId and accountUserId if present.
   *
   * @param accountUsers List of AccountUsers assigned to Account.
   * @param accountUserId ID of selected AccountUser.
   * @return {@link List} of UUID which contains account users IDs.
   */
  private List<UUID> selectAccountUsers(List<AccountUserResponse> accountUsers,
      UUID accountUserId) {
    if (accountUserId == null) {
      return accountUsers.stream()
          .map(AccountUserResponse::getAccountUserId)
          .collect(Collectors.toList());
    } else {
      return accountUsers.stream()
          .filter(
              accountUserResponse -> accountUserResponse.getAccountUserId().equals(accountUserId))
          .map(AccountUserResponse::getAccountUserId)
          .collect(Collectors.toList());
    }
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
   * Method which enrich provided {@code CsvEntrantPayment} list with data from External APIs
   * and collect it as {@code EnrichedCsvEntrantPayment}.
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
   * Method which enrich provided {@code CsvEntrantPayment} with data from External APIs
   * and returns it as {@code EnrichedCsvEntrantPayment}.
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

    if (paymentModification.isPresent()) {
      enrichedCsvEntrantPayment
          .status(paymentModification.get().getEntrantPaymentStatus())
          .dateReceivedFromLa(paymentModification.get().getModificationTimestamp().toLocalDate())
          .caseReference(paymentModification.get().getCaseReference());
    }
    return enrichedCsvEntrantPayment.build();
  }

  /**
   * Generates csv row from {@code EnrichedCsvEntrantPayment}.
   */
  private String createCsvRow(EnrichedCsvEntrantPayment enrichedCsvEntrantPayment) {
    return String.join(",",
        safeToString(enrichedCsvEntrantPayment.getDateOfPayment()),
        safeToString(enrichedCsvEntrantPayment.getPaymentMadeBy()),
        safeToString(enrichedCsvEntrantPayment.getCazName()),
        safeToString(enrichedCsvEntrantPayment.getVrn()),
        safeToString(enrichedCsvEntrantPayment.getDateOfEntry()),
        toFormattedPounds(enrichedCsvEntrantPayment.getCharge()),
        safeToString(enrichedCsvEntrantPayment.getPaymentReference()),
        safeToString(enrichedCsvEntrantPayment.getPaymentProviderId()),
        safeToString(enrichedCsvEntrantPayment.getEntriesCount()),
        toFormattedPounds(enrichedCsvEntrantPayment.getTotalPaid()),
        safeToString(enrichedCsvEntrantPayment.getStatus()),
        safeToString(enrichedCsvEntrantPayment.getDateReceivedFromLa()),
        safeToString(enrichedCsvEntrantPayment.getCaseReference())
    );
  }

  /**
   * Method used to return empty string if object was null.
   */
  private String safeToString(Object o) {
    return o == null ? "" : o.toString();
  }

  /**
   * Converts pennies to its string representation in pounds.
   */
  final String toFormattedPounds(int amountInPennies) {
    double amountInPounds = toPounds(amountInPennies);
    return "£" + String.format(Locale.UK, "%.2f", amountInPounds);
  }

  /**
   * Converts pennies ({@code amountInPennies}) to pounds.
   */
  final double toPounds(int amountInPennies) {
    return currencyFormatter.parsePennies(amountInPennies);
  }

  /**
   * Method for retrieving a payer name.
   */
  public String getPayerName(UUID userId, List<AccountUserResponse> accountUsers) {
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
        .findFirst();
  }
}
