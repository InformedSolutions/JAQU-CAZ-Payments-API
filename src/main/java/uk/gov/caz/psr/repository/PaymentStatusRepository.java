package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

@Repository
@AllArgsConstructor
public class PaymentStatusRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final PaymentStatusRowMapper ROW_MAPPER =
      new PaymentStatusRowMapper();

  static final String SELECT_BY_ENTRY_DATE_AND_VRN_AND_CAZ_ID_SQL = "SELECT "
      + "entrant_payment.payment_status, "
      + "entrant_payment.case_reference, "
      + "payment.payment_provider_id, "
      + "payment.central_reference_number, "
      + "payment.payment_method, "
      + "payment.payment_provider_mandate_id "
      + "FROM caz_payment.t_clean_air_zone_entrant_payment entrant_payment "
      + "LEFT OUTER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
      + "ON entrant_payment.clean_air_zone_entrant_payment_id = "
      + "entrant_payment_match.clean_air_zone_entrant_payment_id "
      + "AND entrant_payment_match.latest = true "
      + "LEFT OUTER JOIN caz_payment.t_payment payment "
      + "ON entrant_payment_match.payment_id = payment.payment_id "
      + "WHERE entrant_payment.clean_air_zone_id = ? AND "
      + "entrant_payment.vrn = ? AND "
      + "entrant_payment.travel_date = ? AND "
      // exclude not captured with failed payment records.
      + "(entrant_payment.vehicle_entrant_captured is true OR "
      + "entrant_payment.update_actor != '" + EntrantPaymentUpdateActor.USER.name() + "' OR "
      + "entrant_payment.payment_status != 'NOT_PAID')";

  /**
   * Finds collection of matching records in join table. To represent the found records we are
   * passing the data to ${@code PaymentStatusResponse} objects.
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

    return jdbcTemplate.query(
        selectByEntryDateAndVrnAndCazId(),
        preparedStatement -> {
          preparedStatement.setObject(1, cazId);
          preparedStatement.setObject(2, vrn);
          preparedStatement.setObject(3, dateOfCazEntry);
        },
        ROW_MAPPER
    );
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link PaymentStatus}
   * class.
   */
  private static class PaymentStatusRowMapper implements RowMapper<PaymentStatus> {

    @Override
    public PaymentStatus mapRow(ResultSet resultSet, int i) throws SQLException {
      String paymentMethod = resultSet.getString("payment_method");
      return PaymentStatus.builder()
          .externalId(resultSet.getString("payment_provider_id"))
          .paymentReference(resultSet.getLong("central_reference_number"))
          .status(InternalPaymentStatus.valueOf(
              resultSet.getString("payment_status")))
          .caseReference(resultSet.getString("case_reference"))
          .paymentProviderMandateId(resultSet.getString("payment_provider_mandate_id"))
          .paymentMethod(paymentMethod == null ? null : PaymentMethod.valueOf(paymentMethod))
          .build();
    }
  }

  /**
   * Returns SQL with SELECT statement. Useful really only for Sonar security scan. User input is
   * sanitized by PreparedStatement at call point.
   */
  private String selectByEntryDateAndVrnAndCazId() {
    return SELECT_BY_ENTRY_DATE_AND_VRN_AND_CAZ_ID_SQL;
  }
}
