package uk.gov.caz.psr.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;

/**
 * A class which handles managing data in {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT} table.
 */
@Repository
public class EntrantPaymentRepository {

  private static final EntrantPaymentRowMapper ROW_MAPPER =
      new EntrantPaymentRowMapper();

  private static final String SELECT_BY_PAYMENT_ID_SQL = "SELECT "
      + "entrant_payment.clean_air_zone_entrant_payment_id, "
      + "entrant_payment.vrn, "
      + "entrant_payment.clean_air_zone_id, "
      + "entrant_payment.travel_date, "
      + "entrant_payment.tariff_code, "
      + "entrant_payment.charge, "
      + "entrant_payment.payment_status, "
      + "entrant_payment.update_actor, "
      + "entrant_payment.vehicle_entrant_captured, "
      + "entrant_payment.case_reference "
      + "FROM caz_payment.t_clean_air_zone_entrant_payment entrant_payment "
      + "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
      + "ON entrant_payment_match.clean_air_zone_entrant_payment_id = "
      + "entrant_payment.clean_air_zone_entrant_payment_id "
      + "WHERE entrant_payment_match.payment_id = ?";

  private static final String SELECT_PAID_SQL = "SELECT "
      + "clean_air_zone_entrant_payment_id, "
      + "vrn, "
      + "clean_air_zone_id, "
      + "travel_date, "
      + "tariff_code, "
      + "charge, "
      + "payment_status, "
      + "vehicle_entrant_captured, "
      + "case_reference "
      + "FROM caz_payment.t_clean_air_zone_entrant_payment "
      + "WHERE travel_date = ? AND clean_air_zone_id = ? AND vrn = ? AND payment_status = ?";

  @VisibleForTesting
  static final String SELECT_BY_VRN_CAZ_ENTRY_DATE_SQL = "SELECT "
      + "clean_air_zone_entrant_payment_id, "
      + "vrn, "
      + "clean_air_zone_id, "
      + "travel_date, "
      + "tariff_code, "
      + "charge, "
      + "payment_status, "
      + "vehicle_entrant_captured, "
      + "case_reference "
      + "FROM caz_payment.t_clean_air_zone_entrant_payment "
      + "WHERE clean_air_zone_id = ? AND vrn = ? AND travel_date = ?";

  //    @VisibleForTesting
  //    static final String SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL = "SELECT "
  //        + "t_clean_air_zone_entrant_payment.payment_id, "
  //        + "t_clean_air_zone_entrant_payment.vehicle_entrant_payment_id, "
  //        + "t_clean_air_zone_entrant_payment.vrn, "
  //        + "t_clean_air_zone_entrant_payment.clean_air_zone_id, "
  //        + "t_clean_air_zone_entrant_payment.travel_date, "
  //        + "t_clean_air_zone_entrant_payment.tariff_code, "
  //        + "t_clean_air_zone_entrant_payment.charge, "
  //        + "t_clean_air_zone_entrant_payment.payment_status, "
  //        + "t_clean_air_zone_entrant_payment.case_reference "
  //        + "FROM t_clean_air_zone_entrant_payment "
  //        + "INNER JOIN payment "
  //        + "ON t_clean_air_zone_entrant_payment.payment_id = payment.payment_id "
  //        + "WHERE t_clean_air_zone_entrant_payment.clean_air_zone_id = ? AND "
  //        + "vehicle_entrant_payment.travel_date = ? AND "
  //        + "vehicle_entrant_payment.payment_status = ? AND "
  //        + "payment.payment_provider_id = ?";

  private static final String UPDATE_SQL = "UPDATE caz_payment.t_clean_air_zone_entrant_payment "
      + "SET payment_status = ?, "
      + "case_reference = ?, "
      + "update_actor = ?, "
      + "vehicle_entrant_captured = ? "
      + "WHERE clean_air_zone_entrant_payment_id = ?";

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;

  /**
   * Creates a new instance of this class.
   */
  public EntrantPaymentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withSchemaName("caz_payment")
        .withTableName("t_clean_air_zone_entrant_payment")
        .usingGeneratedKeyColumns("clean_air_zone_entrant_payment_id")
        .usingColumns("vrn", "clean_air_zone_id", "travel_date", "tariff_code", "charge",
            "payment_status", "update_actor", "vehicle_entrant_captured");
  }

  /**
   * Inserts the passed {@code vehicleEntrantPayments} into the database.
   *
   * @param entrantPayments A list of {@link EntrantPayment} instances.
   * @return A list of {@link EntrantPayment} with their internal identifiers set.
   * @throws NullPointerException if {@code vehicleEntrantPayments} is null.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} is empty.
   * @throws IllegalArgumentException if {@code vehicleEntrantPayments} contains at least one
   *     object whose payment id is null.
   */
  @Transactional
  public List<EntrantPayment> insert(List<EntrantPayment> entrantPayments) {
    Preconditions.checkNotNull(entrantPayments, "Entrant payments cannot be null");
    Preconditions.checkArgument(!entrantPayments.isEmpty(), "Entrant payments "
        + "cannot be empty");
    // TODO check if payments assigned??

    List<EntrantPayment> result = new ArrayList<>(entrantPayments.size());
    for (EntrantPayment entrantPayment : entrantPayments) {
      KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
          toSqlParameters(entrantPayment));
      UUID id = (UUID) keyHolder.getKeys().get("clean_air_zone_entrant_payment_id");
      result.add(entrantPayment.toBuilder().cleanAirZoneEntrantPaymentId(id).build());
    }
    return result;
  }

  /**
   * Converts the passed {@code entrantPayment} to a map of parameters that will be used for
   * database operations.
   */
  private MapSqlParameterSource toSqlParameters(EntrantPayment entrantPayment) {
    return new MapSqlParameterSource()
        .addValue("vrn", entrantPayment.getVrn())
        .addValue("clean_air_zone_id", entrantPayment.getCleanAirZoneId())
        .addValue("travel_date", entrantPayment.getTravelDate())
        .addValue("tariff_code", entrantPayment.getTariffCode())
        .addValue("charge", entrantPayment.getCharge())
        .addValue("payment_status", entrantPayment.getInternalPaymentStatus().name())
        .addValue("vehicle_entrant_captured", entrantPayment.isVehicleEntrantCaptured())
        .addValue("update_actor", entrantPayment.getUpdateActor());
  }

  /**
   * Updates the database with the passed {@link EntrantPayment} instances.
   *
   * @param entrantPayments A list of {@link EntrantPayment} which are to be updated in the
   *     database.
   * @throws NullPointerException if {@code EntrantPayment} is null
   */
  @Transactional
  public void update(List<EntrantPayment> entrantPayments) {
    Preconditions.checkNotNull(entrantPayments, "Entrant payments cannot be null");

    for (EntrantPayment entrantPayment : entrantPayments) {
      update(entrantPayment);
    }
  }

  /**
   * Updates the database with the passed {@link EntrantPayment} instance.
   *
   * @param entrantPayment An instance of {@link EntrantPayment} which is to be updated in the
   *     database.
   * @throws NullPointerException if {@code entrantPayment} is null.
   */
  public void update(EntrantPayment entrantPayment) {
    Preconditions.checkNotNull(entrantPayment, "Entrant payments cannot be null");

    jdbcTemplate.update(UPDATE_SQL, preparedStatementSetter -> {
      preparedStatementSetter.setString(1, entrantPayment.getInternalPaymentStatus()
          .name());
      preparedStatementSetter.setString(2, entrantPayment.getCaseReference());
      preparedStatementSetter.setString(3, entrantPayment.getUpdateActor().name());
      preparedStatementSetter.setBoolean(4, entrantPayment.isVehicleEntrantCaptured());
      preparedStatementSetter.setObject(5, entrantPayment.getCleanAirZoneEntrantPaymentId());
    });
  }

  /**
   * Finds all {@link EntrantPayment} entities by the passed {@code paymentId} (an identifier of the
   * payment for which entries in T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT table are populated).
   * If none is find, an empty list is returned.
   *
   * @param paymentId The payment identifier against which the search is to be performed.
   * @return A list of matching {@link EntrantPayment} entities.
   * @throws NullPointerException if {@code paymentId} is null.
   */
  public List<EntrantPayment> findByPaymentId(UUID paymentId) {
    Preconditions.checkNotNull(paymentId, "'paymentId' cannot be null");
    return jdbcTemplate.query(SELECT_BY_PAYMENT_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, paymentId), ROW_MAPPER);
  }

  /**
   * Finds single {@link EntrantPayment} entity for the passed {@link VehicleEntrant} whose status
   * is equal to 'PAID'. If none is found, {@link Optional#empty()} is returned.
   *
   * @param vehicleEntrant provided VehicleEntrant object.
   * @return An optional containing {@link EntrantPayment} entity.
   * @throws NullPointerException if {@code vehicleEntrant} is null.
   */
  public Optional<EntrantPayment> findSuccessfullyPaid(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");

    List<EntrantPayment> results = jdbcTemplate.query(SELECT_PAID_SQL,
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
   * Finds single of {@link EntrantPayment} entity for the passed params. If none is found,
   * {@link Optional#empty()} is returned. If more then one found throws {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param cleanZoneId provided Clean Air Zone ID.
   * @param vrn provided VRN number
   * @param cazEntryDate Date of entry to provided CAZ
   * @return list of found {@link EntrantPayment}.
   * @throws NullPointerException if {@code cleanZoneId} is null.
   * @throws NullPointerException if {@code cazEntryDate} is null.
   * @throws IllegalArgumentException if {@code vrn} is empty.
   * @throws NotUniqueVehicleEntrantPaymentFoundException if method found more than one
   *     VehicleEntrantPayment.
   */
  public Optional<EntrantPayment> findOneByVrnAndCazEntryDate(UUID cleanZoneId,
      String vrn, LocalDate cazEntryDate) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDate, "cazEntryDate cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<EntrantPayment> results = jdbcTemplate
        .query(SELECT_BY_VRN_CAZ_ENTRY_DATE_SQL,
            preparedStatement -> {
              preparedStatement.setObject(1, cleanZoneId);
              preparedStatement.setString(2, vrn);
              preparedStatement.setObject(3, cazEntryDate);
            },
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new NotUniqueVehicleEntrantPaymentFoundException(vrn,
          "Not able to find unique EntrantPayment");
    }
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * Finds single of {@link EntrantPayment} entity for the passed params. If none is found, {@link
   * Optional#empty()} is returned. If more then one found throws {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param cleanZoneId provided Clean Air Zone ID
   * @param cazEntryDate Date of entry to provided CAZ
   * @param externalPaymentId Payment Id from GOV.UK PAY
   * @return list of found {@link EntrantPayment}.
   * @throws NullPointerException if {@code cleanZoneId} is null.
   * @throws NullPointerException if {@code cazEntryDate} is null.
   * @throws IllegalArgumentException if {@code vrn} is empty.
   * @throws NotUniqueVehicleEntrantPaymentFoundException if method found more than one
   *     VehicleEntrantPayment.
   */
  public Optional<EntrantPayment> findOnePaidByCazEntryDateAndExternalPaymentId(
      UUID cleanZoneId, LocalDate cazEntryDate, String externalPaymentId) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkNotNull(cazEntryDate, "cazEntryDate cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(externalPaymentId),
        "externalPaymentId cannot be empty");
    //    TODO: Fix with the payment updates CAZ-1716
    //    List<CazEntrantPayment> results = jdbcTemplate
    //        .query(SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL,
    //            preparedStatement -> {
    //              preparedStatement.setObject(1, cleanZoneId);
    //              preparedStatement.setObject(2, cazEntryDate);
    //              preparedStatement.setString(3, InternalPaymentStatus.PAID.name());
    //              preparedStatement.setString(4, externalPaymentId);
    //            },
    //            ROW_MAPPER
    //        );
    //    if (results.size() > 1) {
    //      throw new NotUniqueVehicleEntrantPaymentFoundException(null,
    //          "Not able to find unique VehicleEntrantPayment");
    //    }
    //    if (results.isEmpty()) {
    //      return Optional.empty();
    //    }
    //    return Optional.of(results.iterator().next());
    return Optional.empty();
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link EntrantPayment}.
   */
  static class EntrantPaymentRowMapper implements RowMapper<EntrantPayment> {

    /**
     * Maps {@link ResultSet} to {@link EntrantPayment}.
     */
    @Override
    public EntrantPayment mapRow(ResultSet resultSet, int i) throws SQLException {
      return EntrantPayment.builder()
          .cleanAirZoneEntrantPaymentId(
              UUID.fromString(resultSet.getString("clean_air_zone_entrant_payment_id")))
          .vrn(resultSet.getString("vrn"))
          .cleanAirZoneId(UUID.fromString(resultSet.getString("clean_air_zone_id")))
          .travelDate(LocalDate.parse(resultSet.getString("travel_date")))
          .tariffCode(resultSet.getString("tariff_code"))
          .charge(resultSet.getInt("charge"))
          .internalPaymentStatus(InternalPaymentStatus.valueOf(
              resultSet.getString("payment_status")))
          .vehicleEntrantCaptured(resultSet.getBoolean("vehicle_entrant_captured"))
          .caseReference(resultSet.getString("case_reference"))
          .updateActor(EntrantPaymentUpdateActor.valueOf(resultSet.getString("update_actor")))
          .build();
    }
  }
}
