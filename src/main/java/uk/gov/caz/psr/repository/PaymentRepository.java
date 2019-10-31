package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
public class PaymentRepository {

  private static final PaymentFindByIdMapper FIND_BY_ID_MAPPER = new PaymentFindByIdMapper();

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;

  static final String SELECT_BY_ID_SQL = "SELECT payment_id, external_payment_id, status, "
      + "case_reference, payment_method, charge_paid, caz_id, correlation_id "
      + "FROM payment "
      + "WHERE payment_id = ?";

  static final String UPDATE_SQL = "UPDATE payment "
      + "SET status = ?, external_payment_id = ?, update_timestamp = CURRENT_TIMESTAMP "
      + "WHERE payment_id = ?";

  /**
   * Creates an instance of {@link PaymentRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}.
   */
  public PaymentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withTableName("payment")
        .usingGeneratedKeyColumns("payment_id")
        .usingColumns("external_payment_id", "status", "payment_method", "charge_paid", "caz_id",
            "correlation_id");
  }


  /**
   * Inserts {@code payment} into database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   * @return An instance of {@link Payment} with its internal identifier set.
   * @throws NullPointerException if {@code payment} or {@link Payment#getCorrelationId()} is null
   * @throws IllegalArgumentException if {@link Payment#getId()} is not null
   */
  public Payment insert(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(payment.getCorrelationId(), "Correlation ID cannot be null");
    Preconditions.checkArgument(payment.getId() == null, "Payment cannot have ID");

    SqlParameterSource parameters = new MapSqlParameterSource()
        .addValue("external_payment_id", payment.getExternalPaymentId())
        .addValue("status", payment.getStatus().name())
        .addValue("payment_method", payment.getPaymentMethod().name())
        .addValue("charge_paid", payment.getChargePaid())
        .addValue("caz_id", payment.getCleanZoneId())
        .addValue("correlation_id", payment.getCorrelationId());

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(parameters);

    return payment.toBuilder()
        .id((UUID) keyHolder.getKeys().get("payment_id"))
        .build();
  }

  /**
   * Update {@code payment} in the database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   * @throws NullPointerException if {@code payment} is null
   */
  public void update(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");

    jdbcTemplate.update(UPDATE_SQL, payment.getStatus().name(), payment.getExternalPaymentId(),
        payment.getId());
  }

  /**
   * Finds a given payment by its internal identifier passed as {@code id}.
   *
   * @param id An internal identifier of the payment.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException if {@code id} is null
   */
  public Optional<Payment> findById(UUID id) {
    Preconditions.checkNotNull(id, "ID cannot be null");

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
          .paymentMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")))
          .chargePaid(resultSet.getInt("charge_paid"))
          .cleanZoneId(UUID.fromString(resultSet.getString("caz_id")))
          .correlationId(resultSet.getString("correlation_id"))
          .build();
    }
  }
}
