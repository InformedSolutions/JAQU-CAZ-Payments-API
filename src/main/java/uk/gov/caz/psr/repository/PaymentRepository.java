package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
@Slf4j
public class PaymentRepository {

  private static final PaymentFindByIdMapper DANGLING_PAYMENT_ROW_MAPPER =
      new PaymentFindByIdMapper(Collections.emptyList());

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;
  private final EntrantPaymentRepository entrantPaymentRepository;

  private static final String TABLE_NAME = "t_payment";
  private static final String SCHEMA_NAME = "CAZ_PAYMENT";

  private static class Columns {

    private static final String PAYMENT_ID = "payment_id";
    private static final String PAYMENT_METHOD = "payment_method";
    private static final String TOTAL_PAID = "total_paid";
    private static final String PAYMENT_PROVIDER_STATUS = "payment_provider_status";
    private static final String REFERENCE_NUMBER = "central_reference_number";
  }

  /**
   * Creates an instance of {@link PaymentRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}.
   * @param entrantPaymentRepository An instance of {@link EntrantPaymentRepository}.
   */
  public PaymentRepository(JdbcTemplate jdbcTemplate,
      EntrantPaymentRepository entrantPaymentRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withSchemaName(SCHEMA_NAME)
        .withTableName(TABLE_NAME)
        .usingGeneratedKeyColumns(Columns.PAYMENT_ID, Columns.REFERENCE_NUMBER)
        .usingColumns(Columns.PAYMENT_METHOD, Columns.TOTAL_PAID, Columns.PAYMENT_PROVIDER_STATUS);
    this.entrantPaymentRepository = entrantPaymentRepository;
  }

  /**
   * Inserts {@code payment} into database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   * @return An instance of {@link Payment} with its internal identifier set.
   * @throws NullPointerException if {@code payment} is null
   * @throws NullPointerException if {@link Payment#getExternalPaymentStatus()} is null
   * @throws IllegalArgumentException if {@link Payment#getId()} is not null
   * @throws IllegalArgumentException if {@link Payment#getEntrantPayments()} ()} is NOT empty
   */
  public Payment insert(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkArgument(payment.getId() == null, "Payment cannot have ID");
    Preconditions.checkNotNull(payment.getExternalPaymentStatus(),
        "External payment status cannot be null");
    Preconditions.checkArgument(payment.getEntrantPayments().isEmpty(),
        "Vehicle entrant payments must be empty");

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
        toSqlParametersForExternalInsert(payment));
    UUID paymentId = (UUID) keyHolder.getKeys().get("payment_id");
    Long referenceNumber = (Long) keyHolder.getKeys().get("central_reference_number");
    return payment.toBuilder()
        .id(paymentId)
        .referenceNumber(referenceNumber)
        .build();
  }

  /**
   * Update {@code payment} in the database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   * @throws NullPointerException if {@code payment} is null
   */
  @Transactional
  public void update(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    log.debug("Updating payment with attributes: {}", payment);

    jdbcTemplate.update(Sql.UPDATE, preparedStatementSetter -> {
      preparedStatementSetter.setObject(1, payment.getExternalId());
      preparedStatementSetter.setObject(2, payment.getSubmittedTimestamp());
      preparedStatementSetter.setString(3, payment.getExternalPaymentStatus().name());
      preparedStatementSetter.setObject(4, payment.getAuthorisedTimestamp());
      preparedStatementSetter.setObject(5, payment.getId());
    });
    entrantPaymentRepository.update(payment.getEntrantPayments());
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

    List<EntrantPayment> entrantPayments =
        entrantPaymentRepository.findByPaymentId(id);
    List<Payment> results = jdbcTemplate.query(Sql.SELECT_BY_ID,
        preparedStatement -> preparedStatement.setObject(1, id),
        new PaymentFindByIdMapper(entrantPayments));
    if (results.isEmpty()) {
      return Optional.empty();
    }
    Payment payment = results.iterator().next();

    return Optional.of(payment);
  }

  /**
   * Finds all unfinished payments done in GOV UK Pay service.
   *
   * @return A list of {@link Payment} which were done in GOV UK Pay service, but were not finished.
   */
  public List<Payment> findDanglingPayments() {
    return jdbcTemplate.query(Sql.SELECT_DANGLING_PAYMENTS, (PreparedStatementSetter) null,
        DANGLING_PAYMENT_ROW_MAPPER);
  }

  /**
   * Converts {@code payment} into a map of attributes which will be saved in the database for an
   * external payment.
   */
  private MapSqlParameterSource toSqlParametersForExternalInsert(Payment payment) {
    return new MapSqlParameterSource().addValue("total_paid", payment.getTotalPaid())
        .addValue("payment_provider_status", payment.getExternalPaymentStatus().name())
        .addValue("payment_method", payment.getPaymentMethod().name());
  }

  /**
   * An inner static class that acts as a 'container' for SQL queries/statements.
   */
  private static class Sql {

    static final String UPDATE = "UPDATE caz_payment.t_payment "
        + "SET payment_provider_id = ?, "
        + "payment_submitted_timestamp = ?, "
        + "payment_provider_status = ?, "
        + "payment_authorised_timestamp = ?, "
        + "update_timestamp = CURRENT_TIMESTAMP "
        + "WHERE payment_id = ?";

    private static final String ALL_PAYMENT_ATTRIBUTES =
        "payment_id, payment_method, payment_provider_id, central_reference_number, "
        + "total_paid, payment_provider_status, payment_submitted_timestamp, " 
        + "payment_authorised_timestamp ";

    static final String SELECT_DANGLING_PAYMENTS =
        "SELECT " + ALL_PAYMENT_ATTRIBUTES + "FROM caz_payment.t_payment " + "WHERE "
            // only GOV UK Pay payment
            + "payment_provider_id IS NOT NULL "
            // only the one which is submitted more than 90 minutes ago; if
            // payment_submitted_timestamp
            // is NULL, the record is not included
            + "AND payment_submitted_timestamp + INTERVAL '90 minutes' < NOW() "
            // only the one whose status is not 'final'
            + "AND payment_provider_status NOT IN ('SUCCESS', 'FAILED', 'CANCELLED', 'ERROR')";

    static final String SELECT_BY_ID =
        "SELECT " + ALL_PAYMENT_ATTRIBUTES + "FROM caz_payment.t_payment " + "WHERE payment_id = ?";
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link Payment}
   * class.
   */
  @Value
  private static class PaymentFindByIdMapper implements RowMapper<Payment> {

    @NonNull
    List<EntrantPayment> entrantPayments;

    @Override
    public Payment mapRow(ResultSet resultSet, int i) throws SQLException {
      String externalStatus = resultSet.getString("payment_provider_status");
      return Payment.builder().id(UUID.fromString(resultSet.getString("payment_id")))
          .paymentMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")))
          .externalId(resultSet.getString("payment_provider_id"))
          .referenceNumber(resultSet.getLong("central_reference_number"))
          .externalPaymentStatus(
              externalStatus == null ? null : ExternalPaymentStatus.valueOf(externalStatus))
          .totalPaid(resultSet.getInt("total_paid"))
          .submittedTimestamp(
              fromTimestampToLocalDateTime(resultSet, "payment_submitted_timestamp"))
          .authorisedTimestamp(
              fromTimestampToLocalDateTime(resultSet, "payment_authorised_timestamp"))
          .entrantPayments(entrantPayments)
          .build();
    }

    private LocalDateTime fromTimestampToLocalDateTime(ResultSet resultSet, String columnLabel)
        throws SQLException {
      Timestamp timestamp = resultSet.getTimestamp(columnLabel);
      return timestamp == null ? null : timestamp.toLocalDateTime();
    }
  }
}