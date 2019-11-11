package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
@Slf4j
public class PaymentRepository {

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;
  private final VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  static final String SELECT_BY_ID_SQL = "SELECT payment_id, payment_method, payment_provider_id, "
      + "total_paid, payment_provider_status, payment_submitted_timestamp, "
      + "payment_authorised_timestamp "
      + "FROM payment "
      + "WHERE payment_id = ?";

  static final String UPDATE_SQL = "UPDATE payment "
      + "SET payment_provider_id = ?, "
      + "payment_submitted_timestamp = ?, "
      + "payment_provider_status = ?, "
      + "payment_authorised_timestamp = ?, "
      + "update_timestamp = CURRENT_TIMESTAMP "
      + "WHERE payment_id = ?";

  /**
   * Creates an instance of {@link PaymentRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}.
   * @param vehicleEntrantPaymentRepository An instance of {@link VehicleEntrantPaymentRepository}.
   */
  public PaymentRepository(JdbcTemplate jdbcTemplate,
      VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withTableName("payment")
        .usingGeneratedKeyColumns("payment_id")
        .usingColumns("payment_method", "total_paid", "payment_provider_status");
    this.vehicleEntrantPaymentRepository = vehicleEntrantPaymentRepository;
  }

  /**
   * Inserts {@code payment} (with the external status set alongside the same status of vehicle
   * entrant payments) into database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   * @return An instance of {@link Payment} with its internal identifier set.
   * @throws NullPointerException if {@code payment} is null
   * @throws NullPointerException if {@link Payment#getExternalPaymentStatus()} is null
   * @throws IllegalArgumentException if {@link Payment#getId()} is not null
   * @throws IllegalArgumentException if {@link Payment#getVehicleEntrantPayments()} ()} do not
   *     have the same payment status.
   */
  public Payment insertWithExternalStatus(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkArgument(payment.getId() == null, "Payment cannot have ID");
    Preconditions.checkNotNull(payment.getExternalPaymentStatus(),
        "External payment status cannot be null");
    Preconditions.checkArgument(haveSameStatus(payment.getVehicleEntrantPayments()),
        "Vehicle entrant payments do not have one common status");

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
        toSqlParametersForExternalInsert(payment));

    UUID paymentId = (UUID) keyHolder.getKeys().get("payment_id");

    List<VehicleEntrantPayment> vehicleEntrantPayments = payment.getVehicleEntrantPayments()
        .stream()
        .map(vehicleEntrantPayment -> updateWithPaymentId(vehicleEntrantPayment, paymentId))
        .collect(Collectors.toList());

    List<VehicleEntrantPayment> vehicleEntrantPaymentsWithIds = vehicleEntrantPaymentRepository
        .insert(vehicleEntrantPayments);

    return payment.toBuilder()
        .id(paymentId)
        .vehicleEntrantPayments(vehicleEntrantPaymentsWithIds)
        .build();
  }

  /**
   * Converts {@code payment} into a map of attributes which will be saved in the database for an
   * external payment.
   */
  private MapSqlParameterSource toSqlParametersForExternalInsert(Payment payment) {
    return new MapSqlParameterSource()
        .addValue("total_paid", payment.getTotalPaid())
        .addValue("payment_provider_status",
            payment.getExternalPaymentStatus().name())
        .addValue("payment_method", payment.getPaymentMethod().name());
  }

  /**
   * Creates a new instance of {@link VehicleEntrantPayment} with {@code
   * VehicleEntrantPayment#paymentId} set to the passed {@code paymentId} value.
   */
  private VehicleEntrantPayment updateWithPaymentId(VehicleEntrantPayment vehicleEntrantPayment,
      UUID paymentId) {
    return vehicleEntrantPayment.toBuilder()
        .paymentId(paymentId)
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

    log.debug("Updating payment with attributes: {}", payment);

    jdbcTemplate.update(UPDATE_SQL,
        preparedStatementSetter -> {
          preparedStatementSetter.setObject(1, payment.getExternalId());
          preparedStatementSetter.setObject(2, payment.getSubmittedTimestamp());
          preparedStatementSetter.setString(3, payment.getExternalPaymentStatus()
              .name());
          preparedStatementSetter.setObject(4, payment.getAuthorisedTimestamp());
          preparedStatementSetter.setObject(5, payment.getId());
        }
    );
    vehicleEntrantPaymentRepository.update(payment.getVehicleEntrantPayments());
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

    List<VehicleEntrantPayment> vehicleEntrantPayments = vehicleEntrantPaymentRepository
        .findByPaymentId(id);
    List<Payment> results = jdbcTemplate.query(SELECT_BY_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, id),
        new PaymentFindByIdMapper(vehicleEntrantPayments)
    );
    if (results.isEmpty()) {
      return Optional.empty();
    }
    Payment payment = results.iterator().next();

    return Optional.of(payment);
  }

  /**
   * Predicate which checks whether all vehicle entrant payments have the same status..
   *
   * @param vehicleEntrantPayments A list of {@link VehicleEntrantPayment}.
   * @return true if {@code vehicleEntrantPayments} is not empty and all vehicle entrant payments
   *     have the same status.
   */
  private boolean haveSameStatus(List<VehicleEntrantPayment> vehicleEntrantPayments) {
    InternalPaymentStatus status = vehicleEntrantPayments.iterator().next()
        .getInternalPaymentStatus();
    return vehicleEntrantPayments.stream()
        .map(VehicleEntrantPayment::getInternalPaymentStatus)
        .allMatch(localStatus -> localStatus == status);
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link Payment}
   * class.
   */

  @Value
  private static class PaymentFindByIdMapper implements RowMapper<Payment> {

    @NonNull
    List<VehicleEntrantPayment> vehicleEntrantPayments;

    @Override
    public Payment mapRow(ResultSet resultSet, int i) throws SQLException {
      String externalStatus = resultSet.getString("payment_provider_status");
      return Payment.builder()
          .id(UUID.fromString(resultSet.getString("payment_id")))
          .paymentMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")))
          .externalId(resultSet.getString("payment_provider_id"))
          .externalPaymentStatus(externalStatus == null
              ? null : ExternalPaymentStatus.valueOf(externalStatus))
          .totalPaid(resultSet.getInt("total_paid"))
          .submittedTimestamp(fromTimestampToLocalDateTime(resultSet,
              "payment_submitted_timestamp"))
          .authorisedTimestamp(fromTimestampToLocalDateTime(resultSet,
              "payment_authorised_timestamp"))
          .vehicleEntrantPayments(vehicleEntrantPayments)
          .build();
    }

    private LocalDateTime fromTimestampToLocalDateTime(ResultSet resultSet,
        String columnLabel) throws SQLException {
      Timestamp timestamp = resultSet.getTimestamp(columnLabel);
      return timestamp == null ? null : timestamp.toLocalDateTime();
    }
  }
}
