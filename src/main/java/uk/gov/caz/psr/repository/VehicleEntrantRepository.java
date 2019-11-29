package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.VehicleEntrant;

/**
 * A class which handles managing data in {@code VEHICLE_ENTRANT} table.
 */
@Repository
@AllArgsConstructor
public class VehicleEntrantRepository {

  private static final FindByMapper FIND_BY_MAPPER = new FindByMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Inserts {@code vehicleEntrant} into database unless it exists. Returns an entity with the
   * generated unique identifier wrapped in the {@link Optional} if the operation succeeded. If it
   * fails (an entity with the tuple (caz_entry_date,caz_id,vrn) exists), then
   * {@link Optional#empty()} is returned.
   *
   * @param vehicleEntrant An entity object which is supposed to be saved in the database.
   * @throws NullPointerException if {@code vehicleEntrant} is null
   * @throws IllegalArgumentException if {@link VehicleEntrant#getId()} is not null
   */
  public Optional<VehicleEntrant> insertIfNotExists(VehicleEntrant vehicleEntrant) {
    Preconditions.checkNotNull(vehicleEntrant, "Vehicle Entrant cannot be null");
    Preconditions.checkArgument(vehicleEntrant.getId() == null, "Vehicle Entrant cannot have ID");

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement preparedStatement = connection
          .prepareStatement(Sql.INSERT_IF_NOT_EXISTS_SQL, new String[] {"vehicle_entrant_id"});
      preparedStatement.setObject(1, vehicleEntrant.getCazEntryTimestamp());
      preparedStatement.setObject(2, vehicleEntrant.getCazEntryDate());
      preparedStatement.setObject(3, vehicleEntrant.getCleanZoneId());
      preparedStatement.setString(4, vehicleEntrant.getVrn());
      return preparedStatement;
    }, keyHolder);

    if (keyHolder.getKeys() == null) {
      return Optional.empty();
    }
    UUID vehicleEntrantId = (UUID) keyHolder.getKeys().get("vehicle_entrant_id");
    return Optional.of(vehicleEntrant.toBuilder().id(vehicleEntrantId).build());
  }

  /**
   * Gets {@code vehicleEntrant} from database if it exists.
   *
   * @param cazEntryDate provided date of vehicle's entry
   * @param cleanZoneId provided cleanZoneId.
   * @param vrn provided vrn.
   * @return An instance of {@link VehicleEntrant} class wrapped in {@link Optional} if the vehicle
   *         entrant is found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException if {@code cazEntryTimestamp} is null
   * @throws NullPointerException if {@code cleanZoneId} is null
   * @throws IllegalArgumentException if {@code vrn} is empty.
   */
  public Optional<VehicleEntrant> findBy(LocalDate cazEntryDate, UUID cleanZoneId, String vrn) {
    Preconditions.checkNotNull(cazEntryDate, "cazEntryDate cannot be null");
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be null or empty");

    List<VehicleEntrant> results = jdbcTemplate.query(Sql.SELECT_BY_SQL, preparedStatement -> {
      preparedStatement.setObject(1, cleanZoneId);
      preparedStatement.setString(2, vrn);
      preparedStatement.setObject(3, cazEntryDate);
    }, FIND_BY_MAPPER);
    if (results.isEmpty()) {
      return Optional.empty();
    }
    VehicleEntrant vehicleEntrant = results.iterator().next();

    return Optional.of(vehicleEntrant);
  }

  /**
   * An inner static class that acts as a 'container' for SQL queries/statements.
   */
  private static class Sql {

    static final String INSERT_IF_NOT_EXISTS_SQL =
        "INSERT INTO vehicle_entrant " + "(caz_entry_timestamp, caz_entry_date, caz_id, vrn) "
            + "VALUES (?, ?, ?, ?) " + "ON CONFLICT DO NOTHING";

    private static final String SELECT_BY_SQL = "SELECT * FROM vehicle_entrant " + "WHERE "
        + "caz_id = ? AND " + "vrn = ? AND " + "caz_entry_date = ?";
  }

  /**
   * A class which maps the results obtained from the database to instances of
   * {@link VehicleEntrant} class.
   */
  private static class FindByMapper implements RowMapper<VehicleEntrant> {

    @Override
    public VehicleEntrant mapRow(ResultSet resultSet, int i) throws SQLException {
      return VehicleEntrant.builder().id(UUID.fromString(resultSet.getString("vehicle_entrant_id")))
          .cleanZoneId(UUID.fromString(resultSet.getString("caz_id")))
          .vrn(resultSet.getString("vrn"))
          .cazEntryTimestamp(resultSet.getObject("caz_entry_timestamp", LocalDateTime.class))
          .cazEntryDate(resultSet.getObject("caz_entry_date", LocalDate.class)).build();
    }
  }
}
