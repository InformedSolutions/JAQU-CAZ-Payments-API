package uk.gov.caz.psr.repository.audit;

import com.google.common.base.Preconditions;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentAuditData;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.model.PaymentModificationStatus;

/**
 * Repository for persisting information about Logged Action.
 *
 * @author informed
 */
@Repository
@AllArgsConstructor
public class PaymentDetailRepository {


  private static final PaymentModificationMapper PAYMENT_MODIFICATION_ROW_MAPPER =
      new PaymentModificationMapper();

  private static final PaymentAuditDataRowMapper PAYMENT_AUDIT_DATA_ROW_MAPPER =
      new PaymentAuditDataRowMapper();

  private static final PaymentModificationStatusRowMapper PAYMENT_MODIFICATION_STATUS_ROW_MAPPER =
      new PaymentModificationStatusRowMapper();

  private final JdbcTemplate jdbcTemplate;


  /**
   * Returns a list of statuses set by the LA for the given set of cleanAirZoneIds, travelDates and
   * paymentIds for a given VRN.
   *
   * @param vrn vehicle registration number
   * @param cazIds list of clean air zone ids
   * @param travelDates list of travel dates
   * @param paymentIds list of payment ids
   * @return List of {@link PaymentAuditData}.
   */
  public List<PaymentAuditData> getPaymentStatusesForVrn(String vrn, Set<UUID> cazIds,
      Set<LocalDate> travelDates, Set<UUID> paymentIds) {
    Preconditions.checkNotNull(vrn, "vrn cannot be null");
    Preconditions.checkNotNull(cazIds, "cazIds cannot be null");
    Preconditions.checkNotNull(travelDates, "travelDates cannot be null");
    Preconditions.checkNotNull(paymentIds, "paymentIds cannot be null");

    return jdbcTemplate.query(connection -> {
          PreparedStatement preparedStatement = connection
              .prepareStatement(Sql.SELECT_PAYMENT_STATUSES_SQL);
          preparedStatement.setString(1, vrn);
          preparedStatement.setArray(2, connection.createArrayOf("uuid", cazIds.toArray()));
          preparedStatement.setArray(3, connection.createArrayOf("date", travelDates.toArray()));
          preparedStatement.setArray(4, connection.createArrayOf("uuid", paymentIds.toArray()));
          return preparedStatement;
        },
        PAYMENT_AUDIT_DATA_ROW_MAPPER
    );
  }

  /**
   * Gets payment statuses audit details for paymentId, updateActor and provided payment statuses.
   *
   * @param paymentIds List of Payment ID
   * @param updateActor Describes which actor is responsible for updating the state of Entrant
   *     Payment
   * @param paymentStatuses List of statuses to get from the DB .
   */
  public List<PaymentModificationStatus> getPaymentStatusesForPaymentIds(Set<UUID> paymentIds,
      EntrantPaymentUpdateActor updateActor, List<InternalPaymentStatus> paymentStatuses) {
    Preconditions.checkNotNull(paymentIds, "paymentIds cannot be null");
    Preconditions.checkNotNull(updateActor, "updateActor cannot be null");
    Preconditions.checkNotNull(paymentStatuses, "paymentStatuses cannot be null");

    return jdbcTemplate.query(connection -> {
          PreparedStatement preparedStatement = connection
              .prepareStatement(Sql.SELECT_PAYMENT_STATUSES_FOR_PAYMENT_ID_SQL);
          preparedStatement.setArray(1, connection.createArrayOf("uuid", paymentIds.toArray()));
          preparedStatement.setArray(2,
              connection.createArrayOf("varchar", paymentStatuses.toArray()));
          preparedStatement.setString(3, updateActor.toString());
          return preparedStatement;
        },
        PAYMENT_MODIFICATION_STATUS_ROW_MAPPER
    );
  }


  /**
   * Gets payment audit details for paymentId, updateActor and provided payment statuses.
   *
   * @param paymentId ID of Payment
   * @param updateActor Describes which actor is responsible for updating the state of Entrant
   *     Payment
   * @param paymentStatuses List of statuses to get from the DB
   * @return list of found {@link PaymentModification}
   */
  public List<PaymentModification> findAllForPaymentHistory(UUID paymentId,
      EntrantPaymentUpdateActor updateActor, List<InternalPaymentStatus> paymentStatuses) {
    Preconditions.checkNotNull(paymentId, "paymentId cannot be null");
    Preconditions.checkNotNull(updateActor, "updateActor cannot be null");
    Preconditions.checkNotNull(paymentStatuses, "paymentStatuses cannot be null");

    return jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          Sql.FIND_ALL_FOR_PAYMENT_HISTORY);
      preparedStatement.setObject(1, paymentId);
      preparedStatement.setString(2, updateActor.toString());
      preparedStatement.setArray(3, connection.createArrayOf("varchar", paymentStatuses.toArray()));
      return preparedStatement;
    }, PAYMENT_MODIFICATION_ROW_MAPPER);
  }

  /**
   * Remove log data that is older then the given date.
   *
   * @param inputDate given date
   */
  public void deleteLogsBeforeDate(LocalDate inputDate) {
    deleteDataBeforeDate("caz_payment_audit.t_clean_air_zone_payment_detail", inputDate);
    try {
      deleteDataBeforeDate("caz_payment_audit.t_clean_air_zone_payment_master", inputDate);
    } catch (Exception ex) {
      // FOREIN KEY VIOLATION happens when there is record(s) in table 
      // t_clean_air_zone_payment_detail that refer to a record in 
      // t_clean_air_zone_payment_master
      resetMasterRecordCreationTime(inputDate);
    }
  }

  private int deleteDataBeforeDate(String tableName, LocalDate inputDate) {
    final String sql = String.format("DELETE FROM %s "
        + "WHERE CAST (date_trunc('day', inserttimestamp) AS date) <= ?", tableName);
    return executeStmt(inputDate, sql);
  }

  private int resetMasterRecordCreationTime(LocalDate inputDate) {
    final String sql = "UPDATE caz_payment_audit.t_clean_air_zone_payment_master as master "
        + "SET inserttimestamp =  (SELECT min(d.inserttimestamp) "
        + "FROM caz_payment_audit.t_clean_air_zone_payment_detail as d "
        + "WHERE d.clean_air_zone_payment_master_id = master.clean_air_zone_payment_master_id "
        + "GROUP BY d.clean_air_zone_payment_master_id) "
        + "WHERE CAST (date_trunc('day', master.inserttimestamp) AS date) <= ?";
    return executeStmt(inputDate, sql);
  }

  private int executeStmt(LocalDate inputDate, final String sql) {
    return jdbcTemplate.update(sql, new PreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps) throws SQLException {
        ps.setDate(1, java.sql.Date.valueOf(inputDate));
      }
    });
  }

  /**
   * An inner static class that acts as a 'container' for SQL queries/statements.
   */
  private static class Sql {

    static final String FIND_ALL_FOR_PAYMENT_HISTORY = "SELECT "
        + "t_detail.charge, "
        + "t_detail.travel_date, "
        + "t_detail.case_reference, "
        + "t_detail.entrant_payment_update_timestamp, "
        + "t_detail.payment_status, "
        + "t_master.vrn "
        + "FROM caz_payment_audit.t_clean_air_zone_payment_detail t_detail "
        + "INNER JOIN caz_payment_audit.t_clean_air_zone_payment_master t_master ON "
        + "t_detail.clean_air_zone_payment_master_id = t_master.clean_air_zone_payment_master_id "
        + "AND t_detail.payment_id = ? "
        + "AND t_detail.update_actor = ? "
        + "AND t_detail.payment_status = any (?) "
        + "ORDER BY t_detail.entrant_payment_update_timestamp ASC;";

    private static final String SELECT_PAYMENT_STATUSES_SQL =
        "SELECT vrn, clean_air_zone_id, travel_date, payment_id, payment_status "
            + "FROM caz_payment_audit.t_clean_air_zone_payment_detail t_detail "
            + "INNER JOIN caz_payment_audit.t_clean_air_zone_payment_master t_master "
            + "ON t_detail.clean_air_zone_payment_master_id = "
            + "t_master.clean_air_zone_payment_master_id "
            + "WHERE t_master.vrn = ? "
            + "AND t_master.clean_air_zone_id = ANY (?) "
            + "AND t_detail.travel_date = ANY (?) "
            + "AND t_detail.payment_id = ANY (?) "
            + "AND update_actor = 'LA'";

    private static final String SELECT_PAYMENT_STATUSES_FOR_PAYMENT_ID_SQL =
        "SELECT t_detail.payment_status, t_detail.payment_id "
            + "FROM caz_payment_audit.t_clean_air_zone_payment_detail t_detail "
            + "WHERE t_detail.payment_id = ANY (?) "
            + "AND t_detail.payment_status = ANY (?) "
            + "AND t_detail.update_actor = ?;";
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link
   * PaymentAuditData}.
   */
  static class PaymentAuditDataRowMapper implements RowMapper<PaymentAuditData> {

    @Override
    public PaymentAuditData mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentAuditData.builder()
          .vrn(resultSet.getString("vrn"))
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .travelDate(LocalDate.parse(resultSet.getString("travel_date")))
          .cleanAirZoneId(UUID.fromString(resultSet.getString("clean_air_zone_id")))
          .paymentStatus(InternalPaymentStatus.valueOf(resultSet.getString("payment_status")))
          .build();
    }
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link
   * PaymentModificationStatus}.
   */
  static class PaymentModificationStatusRowMapper implements RowMapper<PaymentModificationStatus> {

    @Override
    public PaymentModificationStatus mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentModificationStatus.builder()
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .paymentStatus(InternalPaymentStatus.valueOf(resultSet.getString("payment_status")))
          .build();
    }
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link
   * PaymentModification} class.
   */
  @Value
  private static class PaymentModificationMapper implements RowMapper<PaymentModification> {

    @Override
    public PaymentModification mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentModification.builder()
          .amount(resultSet.getInt("charge"))
          .caseReference(resultSet.getString("case_reference"))
          .entrantPaymentStatus(resultSet.getString("payment_status"))
          .travelDate(resultSet.getDate("travel_date").toLocalDate())
          .modificationTimestamp(
              resultSet.getTimestamp("entrant_payment_update_timestamp").toLocalDateTime())
          .vrn(resultSet.getString("vrn"))
          .build();
    }
  }
}