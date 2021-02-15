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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatusAuditData;

/**
 * Repository for persisting information about Logged Action.
 *
 * @author informed
 */
@Repository
@AllArgsConstructor
public class PaymentDetailRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final PaymentStatusAuditDataRowMapper ROW_MAPPER =
      new PaymentStatusAuditDataRowMapper();

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

  /**
   * Returns a list of statuses set by the LA for the given set of cleanAirZoneIds, travelDates and
   * paymentIds for a given VRN.
   *
   * @param vrn vehicle registration number
   * @param cazIds list of clean air zone ids
   * @param travelDates list of travel dates
   * @param paymentIds list of payment ids
   * @return List of {@link PaymentStatusAuditData}.
   */
  public List<PaymentStatusAuditData> getPaymentStatuses(String vrn, Set<UUID> cazIds,
      Set<LocalDate> travelDates, Set<UUID> paymentIds) {
    Preconditions.checkNotNull(vrn, "vrn cannot be null");
    Preconditions.checkNotNull(cazIds, "cazIds cannot be null");
    Preconditions.checkNotNull(travelDates, "travelDates cannot be null");
    Preconditions.checkNotNull(paymentIds, "paymentIds cannot be null");

    return jdbcTemplate.query(connection -> {
          PreparedStatement preparedStatement = connection
              .prepareStatement(SELECT_PAYMENT_STATUSES_SQL);
          preparedStatement.setString(1, vrn);
          preparedStatement.setArray(2, connection.createArrayOf("uuid", cazIds.toArray()));
          preparedStatement.setArray(3, connection.createArrayOf("date", travelDates.toArray()));
          preparedStatement.setArray(4, connection.createArrayOf("uuid", paymentIds.toArray()));
          return preparedStatement;
        },
        ROW_MAPPER
    );
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
   * A class that maps the row returned from the database to an instance of {@link
   * PaymentStatusAuditData}.
   */
  static class PaymentStatusAuditDataRowMapper implements RowMapper<PaymentStatusAuditData> {

    @Override
    public PaymentStatusAuditData mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentStatusAuditData.builder()
          .vrn(resultSet.getString("vrn"))
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .travelDate(LocalDate.parse(resultSet.getString("travel_date")))
          .cleanAirZoneId(UUID.fromString(resultSet.getString("clean_air_zone_id")))
          .paymentStatus(InternalPaymentStatus.valueOf(resultSet.getString("payment_status")))
          .build();
    }
  }
}