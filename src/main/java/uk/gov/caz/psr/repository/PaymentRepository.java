package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import uk.gov.caz.psr.service.exception.ReferenceNumberNotFound;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
@Slf4j
public class PaymentRepository {

  private static final PaymentFindByIdMapper PAYMENT_ROW_MAPPER =
      new PaymentFindByIdMapper(Collections.emptyList());

  private static final RowMapper<UUID> PAYMENT_ID_MAPPER = (rs, i) -> UUID
      .fromString(rs.getString(1));

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
    private static final String PAYMENT_PROVIDER_ID = "payment_provider_id";
    private static final String REFERENCE_NUMBER = "central_reference_number";
    private static final String USER_ID = "user_id";
    private static final String OPERATOR_ID = "operator_id";
    private static final String PAYMENT_MANDATE_PROVIDER_ID = "payment_provider_mandate_id";
    private static final String TELEPHONE_PAYMENT = "telephone_payment";
    private static final String PAYMENT_SUBMITTED_TIMESTAMP = "payment_submitted_timestamp";
    private static final String PAYMENT_AUTHORISED_TIMESTAMP = "payment_authorised_timestamp";
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
        .usingColumns(Columns.PAYMENT_METHOD, Columns.TOTAL_PAID, Columns.PAYMENT_PROVIDER_STATUS,
            Columns.USER_ID, Columns.PAYMENT_MANDATE_PROVIDER_ID, Columns.TELEPHONE_PAYMENT,
            Columns.OPERATOR_ID);
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
    checkInsertPreconditions(payment);

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
        toSqlParametersForInsert(payment));
    UUID paymentId = (UUID) keyHolder.getKeys().get("payment_id");
    Long referenceNumber = (Long) keyHolder.getKeys().get("central_reference_number");
    return payment.toBuilder()
        .id(paymentId)
        .referenceNumber(referenceNumber)
        .build();
  }

  /**
   * Checks preconditions for an insert operation.
   */
  private void checkInsertPreconditions(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkArgument(payment.getId() == null, "Payment cannot have ID");
    Preconditions.checkNotNull(payment.getExternalPaymentStatus(),
        "External payment status cannot be null");
    Preconditions.checkArgument(payment.getEntrantPayments().isEmpty(),
        "Vehicle entrant payments must be empty");
    Preconditions.checkArgument(payment.isTelephonePayment()
        || Objects.isNull(payment.getOperatorId()), "Operator ID must be null if "
        + "telephonePayment is false");
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
   * Finds a given payment by central reference number passed as {@code referenceNumber}.
   *
   * @param referenceNumber central reference number.
   * @return An instance of {@link Payment} class wrapped in {@link Optional} if the payment is
   *     found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException if {@code referenceNumber} is null
   */
  public Optional<Payment> findByReferenceNumber(Long referenceNumber) {
    Preconditions.checkNotNull(referenceNumber, "referenceNumber cannot be null");

    UUID paymentId = jdbcTemplate.query(Sql.SELECT_PAYMENT_ID_BY_REFERENCE_NUMBER,
        rs -> rs.next() ? PAYMENT_ID_MAPPER.mapRow(rs, 1) : null,
        referenceNumber);

    if (paymentId == null) {
      throw new ReferenceNumberNotFound();
    }

    return findById(paymentId);
  }

  /**
   * Finds the latest {@link Payment} associated with the {@link EntrantPayment} by the entrant
   * payment's id.
   *
   * @return {@link Optional#empty()} if payment is not found, {@link Payment} wrapped in {@link
   *     Optional} otherwise.
   * @throws IllegalStateException if more than one payment is found.
   */
  public Optional<Payment> findByEntrantPayment(UUID entrantPaymentId) {
    List<Payment> result = jdbcTemplate.query(Sql.FIND_BY_ENTRANT_PAYMENT_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, entrantPaymentId),
        PAYMENT_ROW_MAPPER
    );

    if (result.size() > 1) {
      throw new IllegalStateException("Found more than one payments for entrant payment id = "
          + entrantPaymentId);
    }
    return result.isEmpty() ? Optional.empty() : Optional.of(result.iterator().next());
  }

  /**
   * Finds all unfinished payments done in GOV UK Pay service.
   *
   * @return A list of {@link Payment} which were done in GOV UK Pay service, but were not finished.
   */
  public List<Payment> findDanglingPayments() {
    return jdbcTemplate.query(Sql.SELECT_DANGLING_PAYMENTS, (PreparedStatementSetter) null,
        PAYMENT_ROW_MAPPER);
  }

  /**
   * Converts {@code payment} into a map of attributes which will be saved in the database for an
   * external payment.
   */
  private MapSqlParameterSource toSqlParametersForInsert(Payment payment) {
    return new MapSqlParameterSource()
        .addValue(Columns.TOTAL_PAID, payment.getTotalPaid())
        .addValue(Columns.TELEPHONE_PAYMENT, payment.isTelephonePayment())
        .addValue(Columns.PAYMENT_PROVIDER_STATUS, payment.getExternalPaymentStatus().name())
        .addValue(Columns.PAYMENT_METHOD, payment.getPaymentMethod().name())
        .addValue(Columns.USER_ID, payment.getUserId())
        .addValue(Columns.OPERATOR_ID, payment.getOperatorId())
        .addValue(Columns.PAYMENT_MANDATE_PROVIDER_ID, payment.getPaymentProviderMandateId());
  }

  /**
   * An inner static class that acts as a 'container' for SQL queries/statements.
   */
  private static class Sql {

    static final String FIND_BY_ENTRANT_PAYMENT_ID_SQL = "SELECT "
        + "payment.payment_id, "
        + "payment.payment_method, "
        + "payment.total_paid, "
        + "payment.telephone_payment, "
        + "payment.payment_submitted_timestamp, "
        + "payment.payment_authorised_timestamp, "
        + "payment.payment_provider_status, "
        + "payment.central_reference_number, "
        + "payment.user_id, "
        + "payment.operator_id, "
        + "payment.payment_provider_mandate_id, "
        + "payment.payment_provider_id "
        + "FROM caz_payment.t_clean_air_zone_entrant_payment entrant_payment "
        + "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
        + "ON entrant_payment.clean_air_zone_entrant_payment_id = "
        + "entrant_payment_match.clean_air_zone_entrant_payment_id "
        + "AND entrant_payment_match.latest IS TRUE "
        + "INNER JOIN caz_payment.t_payment payment "
        + "ON entrant_payment_match.payment_id = payment.payment_id "
        + "WHERE entrant_payment.clean_air_zone_entrant_payment_id = ?";

    static final String UPDATE = "UPDATE caz_payment.t_payment "
        + "SET payment_provider_id = ?, "
        + "payment_submitted_timestamp = ?, "
        + "payment_provider_status = ?, "
        + "payment_authorised_timestamp = ?, "
        + "update_timestamp = CURRENT_TIMESTAMP "
        + "WHERE payment_id = ?";

    private static final String ALL_PAYMENT_ATTRIBUTES =
        "payment_id, payment_method, payment_provider_id, central_reference_number, "
        + "total_paid, payment_provider_status, user_id, operator_id, payment_provider_mandate_id,"
        + " payment_submitted_timestamp, payment_authorised_timestamp, telephone_payment ";

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

    static final String SELECT_PAYMENT_ID_BY_REFERENCE_NUMBER =
        "SELECT payment_id FROM caz_payment.t_payment WHERE central_reference_number = ?";
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
      String externalStatus = resultSet.getString(Columns.PAYMENT_PROVIDER_STATUS);
      return Payment.builder()
          .id(UUID.fromString(resultSet.getString(Columns.PAYMENT_ID)))
          .paymentMethod(PaymentMethod.valueOf(resultSet.getString(Columns.PAYMENT_METHOD)))
          .externalId(resultSet.getString(Columns.PAYMENT_PROVIDER_ID))
          .referenceNumber(resultSet.getLong(Columns.REFERENCE_NUMBER))
          .externalPaymentStatus(
              externalStatus == null ? null : ExternalPaymentStatus.valueOf(externalStatus))
          .totalPaid(resultSet.getInt(Columns.TOTAL_PAID))
          .submittedTimestamp(
              fromTimestampToLocalDateTime(resultSet, Columns.PAYMENT_SUBMITTED_TIMESTAMP))
          .authorisedTimestamp(
              fromTimestampToLocalDateTime(resultSet, Columns.PAYMENT_AUTHORISED_TIMESTAMP))
          .userId(nullIfAbsentOrUuidFrom(resultSet.getString(Columns.USER_ID)))
          .operatorId(nullIfAbsentOrUuidFrom(resultSet.getString(Columns.OPERATOR_ID)))
          .paymentProviderMandateId(resultSet.getString(Columns.PAYMENT_MANDATE_PROVIDER_ID))
          .telephonePayment(resultSet.getBoolean(Columns.TELEPHONE_PAYMENT))
          .entrantPayments(entrantPayments)
          .build();
    }

    private UUID nullIfAbsentOrUuidFrom(String identifier) {
      return identifier == null ? null : UUID.fromString(identifier);
    }

    private LocalDateTime fromTimestampToLocalDateTime(ResultSet resultSet, String columnLabel)
        throws SQLException {
      Timestamp timestamp = resultSet.getTimestamp(columnLabel);
      return timestamp == null ? null : timestamp.toLocalDateTime();
    }
  }
}