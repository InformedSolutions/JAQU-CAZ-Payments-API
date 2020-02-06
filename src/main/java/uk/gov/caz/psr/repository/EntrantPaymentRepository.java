package uk.gov.caz.psr.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.sql.PreparedStatement;
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
import org.springframework.util.CollectionUtils;
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

  private static class EntrantPaymentColumns {

    static final String COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID = "clean_air_zone_entrant_payment_id";
    static final String COL_VRN = "vrn";
    static final String COL_CLEAN_AIR_ZONE_ID = "clean_air_zone_id";
    static final String COL_TRAVEL_DATE = "travel_date";
    static final String COL_TARIFF_CODE = "tariff_code";
    static final String COL_CHARGE = "charge";
    static final String COL_PAYMENT_STATUS = "payment_status";
    static final String COL_UPDATE_ACTOR = "update_actor";
    static final String COL_VEHICLE_ENTRANT_CAPTURED = "vehicle_entrant_captured";
    static final String COL_CASE_REFERENCE = "case_reference";
  }

  private static class EntrantPaymentMatchColumns {

    static final String COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID = "clean_air_zone_entrant_payment_id";
    static final String COL_PAYMENT_ID = "payment_id";
  }

  private static String selectAllColumns() {
    return "SELECT ep." + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID + ", "
        + "ep." + EntrantPaymentColumns.COL_VRN + ", "
        + "ep." + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + ", "
        + "ep." + EntrantPaymentColumns.COL_TRAVEL_DATE + ", "
        + "ep." + EntrantPaymentColumns.COL_TARIFF_CODE + ", "
        + "ep." + EntrantPaymentColumns.COL_CHARGE + ", "
        + "ep." + EntrantPaymentColumns.COL_PAYMENT_STATUS + ", "
        + "ep." + EntrantPaymentColumns.COL_UPDATE_ACTOR + ", "
        + "ep." + EntrantPaymentColumns.COL_VEHICLE_ENTRANT_CAPTURED + ", "
        + "ep." + EntrantPaymentColumns.COL_CASE_REFERENCE + " "
        + "FROM caz_payment.t_clean_air_zone_entrant_payment ep ";
  }
  
  private static String selectCount() {
    return "SELECT COUNT(*) FROM caz_payment.t_clean_air_zone_entrant_payment";
  }

  private static final String SELECT_BY_PAYMENT_ID_SQL =
      selectAllColumns()
          + "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match entrant_payment_match "
          + "ON entrant_payment_match."
          + EntrantPaymentMatchColumns.COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID + " = "
          + "ep." + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID
          + " WHERE entrant_payment_match." + EntrantPaymentMatchColumns.COL_PAYMENT_ID + " = ?";

  private static final String SELECT_PAID_SQL =
      selectAllColumns() + " WHERE "
          + EntrantPaymentColumns.COL_TRAVEL_DATE + " = ? AND "
          + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + " = ? AND "
          + EntrantPaymentColumns.COL_VRN + " = ? AND "
          + EntrantPaymentColumns.COL_PAYMENT_STATUS + " = ?";

  @VisibleForTesting
  static final String SELECT_BY_VRN_CAZ_ENTRY_DATE_SQL =
      selectAllColumns() + " WHERE "
          + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + " = ? AND "
          + EntrantPaymentColumns.COL_VRN + " = ? AND "
          + EntrantPaymentColumns.COL_TRAVEL_DATE + " = ?";

  private static final String SELECT_BY_VRN_CAZ_ENTRY_DATES_SQL =
      selectAllColumns() + " WHERE "
          + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + " = ? AND "
          + EntrantPaymentColumns.COL_VRN + " = ? AND "
          + EntrantPaymentColumns.COL_TRAVEL_DATE + " = ANY (?)";

  private static final String SELECT_BY_VRN_CAZ_SQL =
      selectCount() + " WHERE "
          + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + " = ? AND "
          + EntrantPaymentColumns.COL_VRN + " = ?";

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
      + "SET " + EntrantPaymentColumns.COL_PAYMENT_STATUS + " = ?, "
      + EntrantPaymentColumns.COL_CASE_REFERENCE + " = ?, "
      + EntrantPaymentColumns.COL_UPDATE_ACTOR + " = ?, "
      + EntrantPaymentColumns.COL_TARIFF_CODE + " = ?, "
      + EntrantPaymentColumns.COL_VEHICLE_ENTRANT_CAPTURED + " = ?, "
      + EntrantPaymentColumns.COL_CHARGE + " = ?, "
      + "update_timestamp = CURRENT_TIMESTAMP "
      + "WHERE " + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID + " = ?";

  private static final String FIND_ALL_PAID_BY_VRN_DATE_RANGE_AND_CAZ_ID =
      selectAllColumns() + " WHERE "
          + EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID + " = ? AND "
          + EntrantPaymentColumns.COL_VRN + " = ? AND "
          + EntrantPaymentColumns.COL_TRAVEL_DATE + " BETWEEN ? AND ? AND "
          + EntrantPaymentColumns.COL_PAYMENT_STATUS + " = "
          + "\'" + InternalPaymentStatus.PAID.toString() + "\'";

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
        .usingColumns(
            EntrantPaymentColumns.COL_VRN,
            EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID,
            EntrantPaymentColumns.COL_TRAVEL_DATE,
            EntrantPaymentColumns.COL_TARIFF_CODE,
            EntrantPaymentColumns.COL_CHARGE,
            EntrantPaymentColumns.COL_PAYMENT_STATUS,
            EntrantPaymentColumns.COL_UPDATE_ACTOR,
            EntrantPaymentColumns.COL_VEHICLE_ENTRANT_CAPTURED
        );
  }

  /**
   * Inserts the passed {@code entrantPayments} into the database.
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
      EntrantPayment insertedEntrantPayment = insert(entrantPayment);
      result.add(insertedEntrantPayment);
    }
    return result;
  }

  /**
   * Inserts the passed {@code entrantPayment} into the database.
   *
   * @param entrantPayment An instance of {@link EntrantPayment}.
   * @return An instance of {@link EntrantPayment} with its internal identifiers set.
   * @throws NullPointerException if {@code entrantPayment} is null.
   * @throws IllegalArgumentException if {@code entrantPayment} has a non-null entrant payment
   *     id.
   */
  public EntrantPayment insert(EntrantPayment entrantPayment) {
    Preconditions.checkNotNull(entrantPayment, "Entrant payment cannot be null");
    Preconditions.checkArgument(entrantPayment.getCleanAirZoneEntrantPaymentId() == null,
        "Entrant payment cannot have non-null ID");

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
        toSqlParameters(entrantPayment));
    UUID id = (UUID) keyHolder.getKeys().get("clean_air_zone_entrant_payment_id");
    return entrantPayment.toBuilder()
        .cleanAirZoneEntrantPaymentId(id)
        .build();
  }

  /**
   * Converts the passed {@code entrantPayment} to a map of parameters that will be used for
   * database operations.
   */
  private MapSqlParameterSource toSqlParameters(EntrantPayment entrantPayment) {
    return new MapSqlParameterSource()
        .addValue(EntrantPaymentColumns.COL_VRN, entrantPayment.getVrn())
        .addValue(EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID, entrantPayment.getCleanAirZoneId())
        .addValue(EntrantPaymentColumns.COL_TRAVEL_DATE, entrantPayment.getTravelDate())
        .addValue(EntrantPaymentColumns.COL_TARIFF_CODE, entrantPayment.getTariffCode())
        .addValue(EntrantPaymentColumns.COL_CHARGE, entrantPayment.getCharge())
        .addValue(EntrantPaymentColumns.COL_PAYMENT_STATUS,
            entrantPayment.getInternalPaymentStatus().name())
        .addValue(EntrantPaymentColumns.COL_VEHICLE_ENTRANT_CAPTURED,
            entrantPayment.isVehicleEntrantCaptured())
        .addValue(EntrantPaymentColumns.COL_UPDATE_ACTOR, entrantPayment.getUpdateActor());
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
      preparedStatementSetter.setString(4, entrantPayment.getTariffCode());
      preparedStatementSetter.setBoolean(5, entrantPayment.isVehicleEntrantCaptured());
      preparedStatementSetter.setInt(6, entrantPayment.getCharge());
      preparedStatementSetter.setObject(7, entrantPayment.getCleanAirZoneEntrantPaymentId());
    });
  }

  /**
   * Finds all {@link EntrantPayment} entities by the passed {@code paymentId} (an identifier of the
   * payment for which entries in T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT table are populated). If none is
   * find, an empty list is returned.
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
   * Finds single of {@link EntrantPayment} entity for the passed params. If none is found, {@link
   * Optional#empty()} is returned. If more then one found throws {@link
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

    List<EntrantPayment> results = jdbcTemplate.query(
        SELECT_BY_VRN_CAZ_ENTRY_DATE_SQL,
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
   * Finds number of instances of a VRN in the EntrantPayment table.
   *
   * @param cleanAirZoneId provided Clean Air Zone ID.
   * @param vrn provided VRN number
   * @return integer
   */
  public Integer findOneByVrnAndCaz(UUID cleanAirZoneId, String vrn) {
    Preconditions.checkNotNull(cleanAirZoneId, "cleanZoneId cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    return jdbcTemplate.queryForObject(
        SELECT_BY_VRN_CAZ_SQL,
        new Object[] {cleanAirZoneId, vrn},
        Integer.class
    );
  }

  /**
   * Finds a list of {@link EntrantPayment}s based on {@code cleanZoneId}, {@code vrn} and {@code
   * cazEntryDates}.
   *
   * @return A {@link List} of matching {@link EntrantPayment}s.
   */
  public List<EntrantPayment> findByVrnAndCazEntryDates(UUID cleanZoneId, String vrn,
      List<LocalDate> cazEntryDates) {
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkArgument(!CollectionUtils.isEmpty(cazEntryDates),
        "cazEntryDate cannot be null or empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<EntrantPayment> results = jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          SELECT_BY_VRN_CAZ_ENTRY_DATES_SQL);
      preparedStatement.setObject(1, cleanZoneId);
      preparedStatement.setString(2, vrn);
      preparedStatement.setArray(3,
          connection.createArrayOf("date", cazEntryDates.toArray()));
      return preparedStatement;
    }, ROW_MAPPER);

    return results;
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
   * Finds collection of paid {@link EntrantPayment} for a specific vrn, cazId and date range. If no
   * element is found empty collection is returned.
   *
   * @param vrn provided VRN number
   * @param startDate provided date from which search is done
   * @param endDate privded date to which search is done
   * @param cleanAirZoneId provided CAZ ID
   * @return a list of found {@EntrantPayment}.
   */
  public List<EntrantPayment> findAllPaidByVrnAndDateRangeAndCazId(String vrn, LocalDate startDate,
      LocalDate endDate, UUID cleanAirZoneId) {
    Preconditions.checkNotNull(cleanAirZoneId, "cleanAirZoneId cannot be null");
    Preconditions.checkNotNull(startDate, "startDate cannot be null");
    Preconditions.checkNotNull(endDate, "endDate cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<EntrantPayment> entrantPayments = jdbcTemplate.query(
        FIND_ALL_PAID_BY_VRN_DATE_RANGE_AND_CAZ_ID,
        preparedStatementSetter -> {
          preparedStatementSetter.setObject(1, cleanAirZoneId);
          preparedStatementSetter.setObject(2, vrn);
          preparedStatementSetter.setObject(3, startDate);
          preparedStatementSetter.setObject(4, endDate);
        }, ROW_MAPPER);

    return entrantPayments;
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
              UUID.fromString(
                  resultSet.getString(EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_ID)))
          .vrn(resultSet.getString(EntrantPaymentColumns.COL_VRN))
          .cleanAirZoneId(
              UUID.fromString(resultSet.getString(EntrantPaymentColumns.COL_CLEAN_AIR_ZONE_ID)))
          .travelDate(LocalDate.parse(resultSet.getString(EntrantPaymentColumns.COL_TRAVEL_DATE)))
          .tariffCode(resultSet.getString(EntrantPaymentColumns.COL_TARIFF_CODE))
          .charge(resultSet.getInt(EntrantPaymentColumns.COL_CHARGE))
          .internalPaymentStatus(InternalPaymentStatus.valueOf(
              resultSet.getString(EntrantPaymentColumns.COL_PAYMENT_STATUS).toUpperCase()))
          .vehicleEntrantCaptured(
              resultSet.getBoolean(EntrantPaymentColumns.COL_VEHICLE_ENTRANT_CAPTURED))
          .updateActor(EntrantPaymentUpdateActor
              .valueOf(resultSet.getString(EntrantPaymentColumns.COL_UPDATE_ACTOR)))
          .caseReference(resultSet.getString(EntrantPaymentColumns.COL_CASE_REFERENCE))
          .build();
    }
  }
}
