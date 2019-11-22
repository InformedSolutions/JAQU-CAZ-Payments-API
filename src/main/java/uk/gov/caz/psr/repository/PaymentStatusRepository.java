package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;

@Repository
@AllArgsConstructor
public class PaymentStatusRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final PaymentStatusRowMapper ROW_MAPPER =
      new PaymentStatusRowMapper();

  static final String SELECT_BY_ENTRY_DATE_AND_VRN_AND_CAZ_ID_SQL = "SELECT "
      + "vehicle_entrant_payment.payment_status, "
      + "vehicle_entrant_payment.case_reference, "
      + "payment.payment_provider_id "
      + "FROM vehicle_entrant_payment "
      + "INNER JOIN payment on vehicle_entrant_payment.payment_id = payment.payment_id "
      + "WHERE vehicle_entrant_payment.caz_id = ? AND "
      + "vehicle_entrant_payment.vrn = ? AND "
      + "vehicle_entrant_payment.travel_date = ?";

  /**
   * Finds collection of matching records in join table. To represent the found records
   * we are passing the data to ${@code PaymentStatusResponse} objects.
   *
   * @param cazId provided clean air zone ID
   * @param vrn provided VRN
   * @param dateOfCazEntry provided date of vehicle entrance to clean air zone
   * @return A collection of matching records.
   */
  public Collection<PaymentStatus> findByCazIdAndVrnAndEntryDate(
      UUID cazId, String vrn, LocalDate dateOfCazEntry) {
    Preconditions.checkNotNull(cazId, "CAZ ID cannot be null");
    Preconditions.checkNotNull(vrn, "VRN cannot be null");
    Preconditions.checkNotNull(dateOfCazEntry, "dateOfCazEntry cannot be null");

    List<PaymentStatus> results = jdbcTemplate.query(
        SELECT_BY_ENTRY_DATE_AND_VRN_AND_CAZ_ID_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, cazId);
          preparedStatement.setObject(2, vrn);
          preparedStatement.setObject(3, dateOfCazEntry);
        },
        ROW_MAPPER
    );

    return results;
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link
   * PaymentStatus} class.
   */
  private static class PaymentStatusRowMapper implements RowMapper<PaymentStatus> {

    @Override
    public PaymentStatus mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentStatus.builder()
          .externalId(resultSet.getString("payment_provider_id"))
          .status(InternalPaymentStatus.valueOf(
              resultSet.getString("payment_status")))
          .caseReference(resultSet.getString("case_reference"))
          .build();
    }
  }
}
