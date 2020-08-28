package uk.gov.caz.psr.repository;

import com.google.common.base.Preconditions;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.PaymentToCleanAirZoneMapping;

/**
 * Repository responsible for getting {@link PaymentToCleanAirZoneMapping} objects.
 */
@Repository
@AllArgsConstructor
public class PaymentToCleanAirZoneMappingRepository {

  private final JdbcTemplate jdbcTemplate;
  private static final PaymentToCleanAirZoneRowMapper ROW_MAPPER =
      new PaymentToCleanAirZoneRowMapper();

  private static final String SELECT_MAPPING_FOR_USER_IDS =
      "SELECT DISTINCT ep.clean_air_zone_id, tp.payment_id FROM caz_payment.t_payment tp "
          + "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match epm "
          + "ON tp.payment_id = epm.payment_id "
          + "INNER JOIN caz_payment.t_clean_air_zone_entrant_payment ep "
          + "ON epm.clean_air_zone_entrant_payment_id = ep.clean_air_zone_entrant_payment_id "
          + "WHERE epm.clean_air_zone_entrant_payment_id = ep.clean_air_zone_entrant_payment_id "
          + "AND tp.user_id = ANY (?)";

  /**
   * Method receives list of userIds and fetches information about their payments and cleanAirZones
   * with which those payments are associated.
   *
   * @param userIds list of user ids.
   * @return list of payment to caz mappings.
   */
  public List<PaymentToCleanAirZoneMapping> getPaymentToCleanAirZoneMapping(List<UUID> userIds) {
    Preconditions.checkNotNull(userIds, "userIds cannot be null.");

    return jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          SELECT_MAPPING_FOR_USER_IDS);
      preparedStatement.setArray(1,
          connection.createArrayOf("uuid", userIds.toArray()));
      return preparedStatement;
    }, ROW_MAPPER);
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link
   * PaymentToCleanAirZoneMapping} class.
   */
  private static class PaymentToCleanAirZoneRowMapper implements
      RowMapper<PaymentToCleanAirZoneMapping> {

    @Override
    public PaymentToCleanAirZoneMapping mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentToCleanAirZoneMapping.builder()
          .cleanAirZoneId(UUID.fromString(resultSet.getString("clean_air_zone_id")))
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .build();
    }
  }
}
