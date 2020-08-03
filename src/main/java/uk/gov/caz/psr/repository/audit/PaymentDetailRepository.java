package uk.gov.caz.psr.repository.audit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * Repository for persisting information about Logged Action.
 * 
 * @author informed
 */
@Repository
@AllArgsConstructor
public class PaymentDetailRepository {
  
  private final JdbcTemplate jdbcTemplate;

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
}