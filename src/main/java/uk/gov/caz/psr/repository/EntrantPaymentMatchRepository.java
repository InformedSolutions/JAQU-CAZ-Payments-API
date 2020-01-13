package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.EntrantPaymentMatch;

/**
 * A class which handles managing data in {@code T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH} table.
 */
@Repository
public class EntrantPaymentMatchRepository {

  private static final String TABLE_NAME = "T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH";
  private static final String SCHEMA_NAME = "CAZ_PAYMENT";

  private static class Columns {

    private static final String ID = "id";
    private static final String ENTRANT_PAYMENT_ID = "clean_air_zone_entrant_payment_id";
    private static final String PAYMENT_ID = "payment_id";
  }

  private final SimpleJdbcInsert simpleJdbcInsert;

  /**
   * Creates an instance of {@link EntrantPaymentMatchRepository}.
   */
  public EntrantPaymentMatchRepository(JdbcTemplate jdbcTemplate) {
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withSchemaName(SCHEMA_NAME)
        .withTableName(TABLE_NAME)
        .usingGeneratedKeyColumns(Columns.ID)
        .usingColumns(Columns.ENTRANT_PAYMENT_ID, Columns.PAYMENT_ID);
  }

  /**
   * Inserts {@code entrantPaymentMatch} into the database.
   */
  public EntrantPaymentMatch insert(EntrantPaymentMatch entrantPaymentMatch) {
    Preconditions.checkNotNull(entrantPaymentMatch, "'entrantPaymentMatch' cannot be null");
    Preconditions.checkArgument(entrantPaymentMatch.getId() == null,
        "'entrantPaymentMatch' cannot have ID");

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(toSqlParameters(
        entrantPaymentMatch));
    return entrantPaymentMatch.toBuilder()
        .id((UUID) keyHolder.getKeys().get(Columns.ID))
        .build();
  }

  /**
   * Converts {@code entrantPaymentMatch} into a map of attributes which will be saved in the
   * database.
   */
  private MapSqlParameterSource toSqlParameters(EntrantPaymentMatch entrantPaymentMatch) {
    return new MapSqlParameterSource()
        .addValue(Columns.ENTRANT_PAYMENT_ID, entrantPaymentMatch.getVehicleEntrantPaymentId())
        .addValue(Columns.PAYMENT_ID, entrantPaymentMatch.getPaymentId());
  }
}
