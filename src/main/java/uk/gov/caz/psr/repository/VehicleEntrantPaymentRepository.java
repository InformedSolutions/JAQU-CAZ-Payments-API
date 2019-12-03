package uk.gov.caz.psr.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;

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

  private static final String SELECT_PAID_SQL = "SELECT "
      + "payment_id, "
      + "vehicle_entrant_payment_id, "
      + "vrn, "
      + "caz_id, "
      + "travel_date, "
      + "charge_paid, "
      + "payment_status, "
      + "case_reference "
      + "FROM vehicle_entrant_payment "
      + "WHERE travel_date = ? AND caz_id = ? AND vrn = ? AND payment_status = ?";

  @VisibleForTesting
  static final String SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL = "SELECT "
      + "payment_id, "
      + "vehicle_entrant_payment_id, "
      + "vrn, "
      + "caz_id, "
      + "travel_date, "
      + "charge_paid, "
      + "payment_status, "
      + "case_reference "
      + "FROM vehicle_entrant_payment "
      + "WHERE caz_id = ? AND vrn = ? AND travel_date = ? AND payment_status = ?";

  @VisibleForTesting
  static final String SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL = "SELECT "
      + "vehicle_entrant_payment.payment_id, "
      + "vehicle_entrant_payment.vehicle_entrant_payment_id, "
      + "vehicle_entrant_payment.vrn, "
      + "vehicle_entrant_payment.caz_id, "
      + "vehicle_entrant_payment.travel_date, "
      + "vehicle_entrant_payment.charge_paid, "
      + "vehicle_entrant_payment.payment_status, "
      + "vehicle_entrant_payment.case_reference "
      + "FROM vehicle_entrant_payment "
      + "INNER JOIN payment ON vehicle_entrant_payment.payment_id = payment.payment_id "
      + "WHERE vehicle_entrant_payment.caz_id = ? AND "
      + "vehicle_entrant_payment.travel_date = ? AND "
      + "vehicle_entrant_payment.payment_status = ? AND "
      + "payment.payment_provider_id = ?";

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
   * @throws NullPointerException     if {@code vehicleEntrantPayments} is null.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} is empty.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} contains at least one object
   *                                  whose payment id is null.
   */
  @Transactional
  public List<VehicleEntrantPayment> insert(List<VehicleEntrantPayment> vehicleEntrantPayments) {
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
        .addValue("payment_status", vehicleEntrantPayment.getInternalPaymentStatus()
            .name());
  }

  /**
   * Updates the database with the passed {@link VehicleEntrantPayment} instances.
   *
   * @param vehicleEntrantPayments A list of {@link VehicleEntrantPayment} which are to be updated
   *                               in the database.
   * @throws NullPointerException if {@code vehicleEntrantPayments} is null
   */
  @Transactional
  public void update(List<VehicleEntrantPayment> vehicleEntrantPayments) {
    Preconditions.checkNotNull(vehicleEntrantPayments, "Vehicle entrant payments cannot be null");

    for (VehicleEntrantPayment vehicleEntrantPayment : vehicleEntrantPayments) {
      update(vehicleEntrantPayment);
    }
  }

  /**
   * Updates the database with the passed {@link VehicleEntrantPayment} instance.
   *
   * @param vehicleEntrantPayment An instance of {@link VehicleEntrantPayment} which is to be
   *                              updated in the database.
   * @throws NullPointerException if {@code vehicleEntrantPayment} is null.
   */
  public void update(VehicleEntrantPayment vehicleEntrantPayment) {
    Preconditions.checkNotNull(vehicleEntrantPayment, "Vehicle entrant payments cannot be null");

    jdbcTemplate.update(UPDATE_SQL, preparedStatementSetter -> {
      preparedStatementSetter.setObject(1, vehicleEntrantPayment.getVehicleEntrantId());
      preparedStatementSetter.setString(2, vehicleEntrantPayment.getInternalPaymentStatus()
          .name());
      preparedStatementSetter.setString(3, vehicleEntrantPayment.getCaseReference());
      preparedStatementSetter.setObject(4, vehicleEntrantPayment.getId());
    });
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
   * Finds single {@link VehicleEntrantPayment} entity for the passed {@link VehicleEntrant} whose
   * status is equal to 'PAID'. If none is found, {@link Optional#empty()} is returned.
   *
   * @param vehicleEntrant provided VehicleEntrant object.
   * @return An optional containing {@link VehicleEntrantPayment} entity.
   * @throws NullPointerException if {@code vehicleEntrant} is null.
   */
  public Optional<VehicleEntrantPayment> findSuccessfullyPaid(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");

    List<VehicleEntrantPayment> results = jdbcTemplate.query(SELECT_PAID_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, vehicleEntrant.getCazEntryDate());
          preparedStatement.setObject(2, vehicleEntrant.getCleanZoneId());
          preparedStatement.setString(3, vehicleEntrant.getVrn());
          preparedStatement.setString(4, InternalPaymentStatus.PAID.name());
        },
        ROW_MAPPER
    );
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * Finds single of {@link VehicleEntrantPayment} entity for the passed params. If none is found,
   * {@link Optional#empty()} is returned. If more then one found throws {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param cleanZoneId  provided Clean Air Zone ID.
   * @param vrn          provided VRN number
   * @param cazEntryDate Date of entry to provided CAZ
   * @return list of found {@link VehicleEntrantPayment}.
   * @throws NullPointerException                         if {@code cleanZoneId} is null.
   * @throws NullPointerException                         if {@code cazEntryDate} is null.
   * @throws IllegalArgumentException                     if {@code vrn} is empty.
   * @throws NotUniqueVehicleEntrantPaymentFoundException if method found more than one
   *                                                      VehicleEntrantPayment.
   */
  public Optional<VehicleEntrantPayment> findOnePaidByVrnAndCazEntryDate(UUID cleanZoneId,
      String vrn, LocalDate cazEntryDate) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDate, "cazEntryDate cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<VehicleEntrantPayment> results = jdbcTemplate
        .query(SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL,
            preparedStatement -> {
              preparedStatement.setObject(1, cleanZoneId);
              preparedStatement.setString(2, vrn);
              preparedStatement.setObject(3, cazEntryDate);
              preparedStatement.setString(4, InternalPaymentStatus.PAID.name());
            },
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new NotUniqueVehicleEntrantPaymentFoundException(vrn,
          "Not able to find unique VehicleEntrantPayment");
    }
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * Finds single of {@link VehicleEntrantPayment} entity for the passed params. If none is found,
   * {@link Optional#empty()} is returned. If more then one found throws {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param cleanZoneId       provided Clean Air Zone ID
   * @param cazEntryDate      Date of entry to provided CAZ
   * @param externalPaymentId Payment Id from GOV.UK PAY
   * @return list of found {@link VehicleEntrantPayment}.
   * @throws NullPointerException                         if {@code cleanZoneId} is null.
   * @throws NullPointerException                         if {@code cazEntryDate} is null.
   * @throws IllegalArgumentException                     if {@code vrn} is empty.
   * @throws NotUniqueVehicleEntrantPaymentFoundException if method found more than one
   *                                                      VehicleEntrantPayment.
   */
  public Optional<VehicleEntrantPayment> findOnePaidByCazEntryDateAndExternalPaymentId(
      UUID cleanZoneId, LocalDate cazEntryDate, String externalPaymentId) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDate, "cazEntryDate cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(externalPaymentId),
        "externalPaymentId cannot be empty");

    List<VehicleEntrantPayment> results = jdbcTemplate
        .query(SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL,
            preparedStatement -> {
              preparedStatement.setObject(1, cleanZoneId);
              preparedStatement.setObject(2, cazEntryDate);
              preparedStatement.setString(3, InternalPaymentStatus.PAID.name());
              preparedStatement.setString(4, externalPaymentId);
            },
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new NotUniqueVehicleEntrantPaymentFoundException(null,
          "Not able to find unique VehicleEntrantPayment");
    }
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link
   * VehicleEntrantPayment}.
   */
  static class VehicleEntrantPaymentRowMapper implements RowMapper<VehicleEntrantPayment> {

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
          .internalPaymentStatus(InternalPaymentStatus.valueOf(
              resultSet.getString("payment_status")))
          .caseReference(resultSet.getString("case_reference"))
          .build();
    }
  }
}
