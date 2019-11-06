package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.VehicleEntrance;

/**
 * A class which handles managing data in {@code VEHICLE_ENTRANCE} table.
 */
@Repository
@AllArgsConstructor
public class VehicleEntranceRepository {

  private static final FindByMapper FIND_BY_MAPPER = new FindByMapper();
  private final JdbcTemplate jdbcTemplate;

  static final String SELECT_BY_SQL = "SELECT * FROM vehicle_entrant "
      + "WHERE caz_id = ? AND vrn = ? AND to_char(caz_entry_timestamp, 'YYYY-MM-DD') = ?";

  /**
   * Inserts {@code vehicleEntrance} into database unless it exists.
   *
   * @param vehicleEntrance An entity object which is supposed to be saved in the database.
   */
  public void insertIfNotExists(VehicleEntrance vehicleEntrance) {
    // TODO to be implemented in CAZ-1238
  }

  /**
   * Gets {@code vehicleEntrance} from database if it exists.
   *
   * @param dateOfEntrance provided dateOfEntrance.
   * @param cleanZoneId    provided cleanZoneId.
   * @param vrn            provided vrn.
   * @return An instance of {@link VehicleEntrance} class wrapped in {@link Optional} if the vehicle
   *     entrance is found, {@link Optional#empty()} otherwise.
   * @throws NullPointerException     if {@code dateOfEntrance} is null
   * @throws NullPointerException     if {@code cleanZoneId} is null
   * @throws IllegalArgumentException if {@code vrn} is empty.
   */
  public Optional<VehicleEntrance> findBy(LocalDate dateOfEntrance, UUID cleanZoneId, String vrn) {
    Preconditions.checkNotNull(dateOfEntrance, "dayOfEntrance cannot be null");
    Preconditions.checkNotNull(cleanZoneId, "cleanZoneId cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be null or empty");

    List<VehicleEntrance> results = jdbcTemplate.query(SELECT_BY_SQL,
        preparedStatement -> {
          preparedStatement.setObject(1, cleanZoneId);
          preparedStatement.setString(2, vrn);
          preparedStatement.setString(3, dateOfEntrance.toString());
        },
        FIND_BY_MAPPER
    );
    if (results.isEmpty()) {
      return Optional.empty();
    }
    VehicleEntrance vehicleEntrance = results.iterator().next();

    return Optional.of(vehicleEntrance);
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link
   * VehicleEntrance} class.
   */
  private static class FindByMapper implements RowMapper<VehicleEntrance> {

    @Override
    public VehicleEntrance mapRow(ResultSet resultSet, int i) throws SQLException {
      return VehicleEntrance.builder()
          .id(UUID.fromString(resultSet.getString("vehicle_entrant_id")))
          .cleanZoneId(UUID.fromString(resultSet.getString("caz_id")))
          .vrn(resultSet.getString("vrn"))
          .cazEntryTimestamp(resultSet.getTimestamp("caz_entry_timestamp").toLocalDateTime())
          .build();
    }
  }
}
