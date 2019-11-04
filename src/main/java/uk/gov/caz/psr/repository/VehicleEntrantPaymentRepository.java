package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

/**
 * A class which handles managing data in {@code VEHICLE_ENTRANT_PAYMENT} table.
 */
@Repository
public class VehicleEntrantPaymentRepository {

  private static final VehicleEntrantPaymentRowMapper ROW_MAPPER =
      new VehicleEntrantPaymentRowMapper();

  private static final String SELECT_BY_PAYMENT_ID_SQL = "SELECT "
      + "payment_id, "
      + "vehicle_entrant_payment_id, "
      + "vrn, "
      + "caz_id, "
      + "travel_date, "
      + "charge_paid, "
      + "payment_status, "
      + "case_reference "
      + "FROM vehicle_entrant_payment "
      + "WHERE payment_id = ?";

  private static final String UPDATE_SQL = "UPDATE vehicle_entrant_payment "
      + "SET vehicle_entrant_id = ?, "
      + "payment_status = ?, "
      + "case_reference = ?, "
      + "update_timestamp = CURRENT_TIMESTAMP "
      + "WHERE vehicle_entrant_payment_id = ?";

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;

  /**
   * Creates a new instance of this class.
   */
  public VehicleEntrantPaymentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withTableName("vehicle_entrant_payment")
        .usingGeneratedKeyColumns("vehicle_entrant_payment_id")
        .usingColumns("payment_id", "vrn", "caz_id", "travel_date", "charge_paid",
            "payment_status");
  }

  /**
   * Inserts the passed {@code vehicleEntrantPayments} into the database.
   *
   * @param vehicleEntrantPayments A list of {@link VehicleEntrantPayment} instances.
   * @return A list of {@link VehicleEntrantPayment} with their internal identifiers set.
   * @throws NullPointerException if {@code vehicleEntrantPayments} is null.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} is empty.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} contains at least one
   *     object whose payment id is null.
   */
  List<VehicleEntrantPayment> insert(List<VehicleEntrantPayment> vehicleEntrantPayments) {
    Preconditions.checkNotNull(vehicleEntrantPayments, "Vehicle entrant payments cannot be null");
    Preconditions.checkArgument(!vehicleEntrantPayments.isEmpty(), "Vehicle entrant payments "
        + "cannot be empty");
    Preconditions.checkArgument(vehicleEntrantPaymentsHaveNonNullPaymentId(vehicleEntrantPayments),
        "Each vehicle entrant payment must have 'payment_id' set");

    List<VehicleEntrantPayment> result = new ArrayList<>(vehicleEntrantPayments.size());
    for (VehicleEntrantPayment vehicleEntrantPayment : vehicleEntrantPayments) {
      KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
          toSqlParameters(vehicleEntrantPayment));
      UUID id = (UUID) keyHolder.getKeys().get("vehicle_entrant_payment_id");
      result.add(vehicleEntrantPayment.toBuilder().id(id).build());
    }
    return result;
  }

  /**
   * Predicate which checks whether all passed {@link VehicleEntrantPayment} instances have non-null
   * payment id.
   */
  private boolean vehicleEntrantPaymentsHaveNonNullPaymentId(
      List<VehicleEntrantPayment> vehicleEntrantPayments) {
    return vehicleEntrantPayments.stream()
        .map(VehicleEntrantPayment::getPaymentId)
        .allMatch(Objects::nonNull);
  }

  /**
   * Converts the passed {@code vehicleEntrantPayment} to a map of parameters that will be used for
   * database operations.
   */
  private MapSqlParameterSource toSqlParameters(VehicleEntrantPayment vehicleEntrantPayment) {
    return new MapSqlParameterSource()
        .addValue("payment_id", vehicleEntrantPayment.getPaymentId())
        .addValue("vrn", vehicleEntrantPayment.getVrn())
        .addValue("caz_id", vehicleEntrantPayment.getCleanZoneId())
        .addValue("travel_date", vehicleEntrantPayment.getTravelDate())
        .addValue("charge_paid", vehicleEntrantPayment.getChargePaid())
        .addValue("payment_status", vehicleEntrantPayment.getStatus().name());
  }

  /**
   * Updates the database with the passed {@link VehicleEntrantPayment} instances.
   *
   * @param vehicleEntrantPayments A list of {@link VehicleEntrantPayment} which are to be
   *     updated in the database.
   * @throws NullPointerException if {@code vehicleEntrantPayments} is null
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} is empty
   */
  public void update(List<VehicleEntrantPayment> vehicleEntrantPayments) {
    Preconditions.checkNotNull(vehicleEntrantPayments, "Vehicle entrant payments cannot be null");
    Preconditions.checkArgument(!vehicleEntrantPayments.isEmpty(),
        "Vehicle entrant payments cannot be empty");

    for (VehicleEntrantPayment vehicleEntrantPayment : vehicleEntrantPayments) {
      jdbcTemplate.update(UPDATE_SQL, preparedStatementSetter -> {
        preparedStatementSetter.setObject(1, vehicleEntrantPayment.getVehicleEntrantId());
        preparedStatementSetter.setString(2, vehicleEntrantPayment.getStatus().name());
        preparedStatementSetter.setString(3, vehicleEntrantPayment.getCaseReference());
        preparedStatementSetter.setObject(4, vehicleEntrantPayment.getId());
      });
    }
  }

  /**
   * Finds all {@link VehicleEntrantPayment} entities by the passed {@code paymentId} (an identifier
   * of the payment for which entries in VEHICLE_ENTRANT_PAYMENT table are populated). If none is
   * find, an empty list is returned.
   *
   * @param paymentId The payment identifier against which the search is to be performed.
   * @return A list of matching {@link VehicleEntrantPayment} entities.
   * @throws NullPointerException if {@code paymentId} is null.
   */
  public List<VehicleEntrantPayment> findByPaymentId(UUID paymentId) {
    Preconditions.checkNotNull(paymentId, "'paymentId' cannot be null");

    return jdbcTemplate.query(SELECT_BY_PAYMENT_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, paymentId),
        ROW_MAPPER
    );
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link
   * VehicleEntrantPayment}.
   */
  private static class VehicleEntrantPaymentRowMapper implements RowMapper<VehicleEntrantPayment> {

    /**
     * Maps {@link ResultSet} to {@link VehicleEntrantPayment}.
     */
    @Override
    public VehicleEntrantPayment mapRow(ResultSet resultSet, int i) throws SQLException {
      return VehicleEntrantPayment.builder()
          .id(UUID.fromString(resultSet.getString("vehicle_entrant_payment_id")))
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .vrn(resultSet.getString("vrn"))
          .cleanZoneId(UUID.fromString(resultSet.getString("caz_id")))
          .travelDate(LocalDate.parse(resultSet.getString("travel_date")))
          .chargePaid(resultSet.getInt("charge_paid"))
          .status(PaymentStatus.valueOf(resultSet.getString("payment_status")))
          .caseReference(resultSet.getString("case_reference"))
          .build();
    }
  }
}
