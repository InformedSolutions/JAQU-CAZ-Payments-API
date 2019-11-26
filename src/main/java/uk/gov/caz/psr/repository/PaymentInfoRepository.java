package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

@Repository
@AllArgsConstructor
public class PaymentInfoRepository {

  private static final String BASE_SQL = ""
      + "SELECT "
      + "   p.payment_provider_id,"
      + "   p.total_paid,"
      + "   p.payment_submitted_timestamp,"
      + "   p.payment_authorised_timestamp,"
      + "   p.payment_provider_status,"
      + "   p.payment_method,"
      + "   vep.vrn,"
      + "   vep.travel_date,"
      + "   vep.charge_paid,"
      + "   vep.payment_status,"
      + "   vep.case_reference "
      + "FROM "
      + "   vehicle_entrant_payment vep,"
      + "   payment p "
      + "WHERE "
      + "   vep.payment_id = p.payment_id AND "
      + "   vep.caz_id = ?";

  private static final String SELECT_BY_VRN_CAZ_AND_DATES_RANGE_SQL = BASE_SQL + " AND "
      + "vep.vrn = ? AND "
      + "vep.travel_date >= ? AND "
      + "vep.travel_date <= ?";

  private static final String SELECT_BY_CAZ_AND_DATES_RANGE_SQL = BASE_SQL + " AND "
      + "vep.travel_date >= ? AND "
      + "vep.travel_date <= ?";

  private static final String SELECT_BY_CAZ_AND_VRN_SQL = BASE_SQL + " AND "
      + "vep.vrn = ?";

  private final JdbcTemplate jdbcTemplate;

  /**
   * Finds all payments and associated vehicle entrant payments by the passed parameters:
   * {@code cleanZoneId} and {@code vrn}.
   */
  public List<Payment> findByCleanZoneIdAndVrn(UUID cleanZoneId, String vrn) {
    checkPreconditions(cleanZoneId, vrn);

    List<PaymentInfoResult> flattenedResult = jdbcTemplate.query(
        SELECT_BY_CAZ_AND_VRN_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, cleanZoneId);
          preparedStatement.setObject(2, vrn);
        },
        PAYMENT_INFO_RESULT_ROW_MAPPER
    );
    return inflateResults(flattenedResult, cleanZoneId);
  }

  /**
   * Finds all payments and associated vehicle entrant payments by the passed parameters:
   * {@code cleanZoneId}, {@code cazEntryDateFrom} and {@code cazEntryDateTo}.
   */
  public List<Payment> findByCleanZoneIdAndEntryDatesRange(UUID cleanZoneId,
      LocalDate cazEntryDateFrom, LocalDate cazEntryDateTo) {
    checkPreconditions(cleanZoneId, cazEntryDateFrom, cazEntryDateTo);

    List<PaymentInfoResult> flattenedResult = jdbcTemplate.query(
        SELECT_BY_CAZ_AND_DATES_RANGE_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, cleanZoneId);
          preparedStatement.setObject(2, cazEntryDateFrom);
          preparedStatement.setObject(3, cazEntryDateTo);
        },
        PAYMENT_INFO_RESULT_ROW_MAPPER
    );
    return inflateResults(flattenedResult, cleanZoneId);
  }

  /**
   * Finds all payments and associated vehicle entrant payments by the passed parameters:
   * {@code cleanZoneId}, {@code vrn}, {@code cazEntryDateFrom} and {@code cazEntryDateTo}.
   */
  public List<Payment> findByCleanZoneIdAndVrnAndEntryDatesRange(UUID cleanZoneId, String vrn,
      LocalDate cazEntryDateFrom, LocalDate cazEntryDateTo) {
    checkPreconditions(vrn, cleanZoneId, cazEntryDateFrom, cazEntryDateTo);

    List<PaymentInfoResult> flattenedResult = jdbcTemplate.query(
        SELECT_BY_VRN_CAZ_AND_DATES_RANGE_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, cleanZoneId);
          preparedStatement.setString(2, vrn);
          preparedStatement.setObject(3, cazEntryDateFrom);
          preparedStatement.setObject(4, cazEntryDateTo);
        },
        PAYMENT_INFO_RESULT_ROW_MAPPER
    );
    return inflateResults(flattenedResult, cleanZoneId);
  }

  private void checkPreconditions(UUID cleanZoneId, LocalDate cazEntryDateFrom,
      LocalDate cazEntryDateTo) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDateFrom, "cazEntryDateFrom cannot be null");
    Preconditions.checkNotNull(cazEntryDateTo, "cazEntryDateTo cannot be null");
  }


  private void checkPreconditions(UUID cleanZoneId, String vrn) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be null or empty");
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
  }

  private void checkPreconditions(String vrn, UUID cleanZoneId, LocalDate cazEntryDateFrom,
      LocalDate cazEntryDateTo) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be null or empty");
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDateFrom, "cazEntryDateFrom cannot be null");
    Preconditions.checkNotNull(cazEntryDateTo, "cazEntryDateTo cannot be null");
  }

  private List<Payment> inflateResults(List<PaymentInfoResult> flattenedResult,
      UUID cleanZoneId) {
    Map<String, Payment> payments = new HashMap<>();
    Map<String, List<VehicleEntrantPayment>> vehicleEntrantPayments = new HashMap<>();
    for (PaymentInfoResult entry : flattenedResult) {
      payments.computeIfAbsent(entry.getExternalId(), unused -> buildPaymentFrom(entry));
      vehicleEntrantPayments.compute(entry.getExternalId(),
          (unused, actualList) -> {
            VehicleEntrantPayment vehicleEntrantPayment = buildVehicleEntrantPaymentFrom(
                cleanZoneId, entry);
            List<VehicleEntrantPayment> list = createIfNull(actualList);
            list.add(vehicleEntrantPayment);
            return list;
          }
      );
    }
    return combine(payments, vehicleEntrantPayments);
  }

  private List<VehicleEntrantPayment> createIfNull(List<VehicleEntrantPayment> actualList) {
    return actualList == null ? new ArrayList<>() : actualList;
  }

  private VehicleEntrantPayment buildVehicleEntrantPaymentFrom(UUID cleanZoneId,
      PaymentInfoResult entry) {
    return VehicleEntrantPayment.builder()
        .vrn(entry.getVrn())
        .cleanZoneId(cleanZoneId)
        .travelDate(entry.getTravelDate())
        .chargePaid(entry.getChargePaid())
        .internalPaymentStatus(entry.getPaymentStatus())
        .caseReference(entry.getCaseReference())
        .build();
  }

  private Payment buildPaymentFrom(PaymentInfoResult entry) {
    return Payment.builder()
        .externalId(entry.getExternalId())
        .externalPaymentStatus(entry.getExternalPaymentStatus())
        .vehicleEntrantPayments(Collections.emptyList())
        .paymentMethod(entry.getPaymentMethod())
        .totalPaid(entry.getTotalPaid())
        .submittedTimestamp(entry.getPaymentSubmittedTimestamp())
        .authorisedTimestamp(entry.getPaymentAuthorisedTimestamp())
        .build();
  }

  private List<Payment> combine(Map<String, Payment> payments,
      Map<String, List<VehicleEntrantPayment>> vehicleEntrantPayments) {
    return payments.values()
        .stream()
        .map(payment -> payment.toBuilder()
            .vehicleEntrantPayments(vehicleEntrantPayments.get(payment.getExternalId()))
            .build())
        .collect(Collectors.toList());
  }

  private static final RowMapper<PaymentInfoResult> PAYMENT_INFO_RESULT_ROW_MAPPER =
      (resultSet, i) -> PaymentInfoResult.builder()
          .externalId(resultSet.getString("payment_provider_id"))
          .totalPaid(resultSet.getInt("total_paid"))
          .paymentSubmittedTimestamp(
              resultSet.getObject("payment_submitted_timestamp", LocalDateTime.class))
          .vrn(resultSet.getString("vrn"))
          .travelDate(resultSet.getObject("travel_date", LocalDate.class))
          .chargePaid(resultSet.getInt("charge_paid"))
          .paymentStatus(InternalPaymentStatus.valueOf(
              resultSet.getString("payment_status")))
          .caseReference(resultSet.getString("case_reference"))
          .paymentMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")))
          .externalPaymentStatus(ExternalPaymentStatus.valueOf(
              resultSet.getString("payment_provider_status")))
          .paymentAuthorisedTimestamp(resultSet.getObject(
              "payment_authorised_timestamp", LocalDateTime.class))
          .build();

  @Value
  @Builder
  private static class PaymentInfoResult {
    String externalId;
    int totalPaid;
    LocalDateTime paymentSubmittedTimestamp;
    LocalDateTime paymentAuthorisedTimestamp;
    PaymentMethod paymentMethod;
    ExternalPaymentStatus externalPaymentStatus;

    String vrn;
    LocalDate travelDate;
    int chargePaid;
    InternalPaymentStatus paymentStatus;
    String caseReference;
  }
}
