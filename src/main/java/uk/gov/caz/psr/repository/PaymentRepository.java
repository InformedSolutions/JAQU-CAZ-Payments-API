package uk.gov.caz.psr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentStatus;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
@AllArgsConstructor
public class PaymentRepository {

  private static final PaymentFindByIdMapper FIND_BY_ID_MAPPER = new PaymentFindByIdMapper();
  private final JdbcTemplate jdbcTemplate;

  static final String SELECT_BY_ID_SQL = "SELECT payment_id, external_payment_id, status, "
      + "case_reference, user_id, payment_method, charge_paid, caz_id "
      + "FROM payment "
      + "WHERE payment_id = ?";

  static final String INSERT_SQL = "insert into PAYMENT (payment_id, external_payment_id, status, "
      + "payment_method, charge_paid, caz_id, correlation_id) values (?, ?, ?, ?, ?, ?, ?)";

  static final String UPDATE_SQL = "UPDATE PAYMENT "
      + "SET status = ?, external_payment_id = ?, update_timestamp = CURRENT_TIMESTAMP"
      + "WHERE payment_id = ?";


  /**
   * Inserts {@code payment} into database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   */
  public void insert(Payment payment) {
    jdbcTemplate.update(INSERT_SQL, payment.getExternalPaymentId(),
        "initiated", "credit_card", payment.getChargePaid(), payment.getCleanZoneId(),
        payment.getCorrelationId());
  }

  /**
   * Update {@code payment} in the database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   */
  public void update(Payment payment) {
    jdbcTemplate.update(UPDATE_SQL, payment.getStatus(), payment.getExternalPaymentId(),
        payment.getId());
  }

  /**
   * Finds a given payment by its internal identifier passed as {@code id}.
   *
   * @param id An internal identifier of the payment.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   */
  public Optional<Payment> findById(UUID id) {
    List<Payment> results = jdbcTemplate.query(SELECT_BY_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, id),
        FIND_BY_ID_MAPPER
    );
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link Payment}
   * class.
   */
  private static class PaymentFindByIdMapper implements RowMapper<Payment> {

    @Override
    public Payment mapRow(ResultSet resultSet, int i) throws SQLException {
      return Payment.builder()
          .id(UUID.fromString(resultSet.getString("payment_id")))
          .externalPaymentId(resultSet.getString("external_payment_id"))
          .status(PaymentStatus.valueOf(resultSet.getString("status")))
          .caseReference(resultSet.getString("case_reference"))
          .userId(toUuidIfPresent(resultSet, "caz_id"))
          .paymentMethod(resultSet.getString("payment_method"))
          .chargePaid(resultSet.getInt("charge_paid"))
          .cleanZoneId(UUID.fromString(resultSet.getString("caz_id")))
          .build();
    }

    /**
     * Converts object returned by {@link ResultSet#getString(String)} to {@link UUID} if it is not
     * {@code null}, returns {@code null} otherwise.
     */
    private UUID toUuidIfPresent(ResultSet resultSet, String columnLabel) throws SQLException {
      String value = resultSet.getString(columnLabel);
      return value == null ? null : UUID.fromString(value);
    }
  }
}

