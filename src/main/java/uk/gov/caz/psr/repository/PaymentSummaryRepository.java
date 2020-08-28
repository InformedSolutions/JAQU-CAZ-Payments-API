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
import uk.gov.caz.psr.model.PaymentSummary;

/**
 * A repository responsible for fetching {@link PaymentSummary} objects.
 */
@Repository
@AllArgsConstructor
public class PaymentSummaryRepository {

  private final JdbcTemplate jdbcTemplate;
  private static final PaymentSummaryMapper ROW_MAPPER = new PaymentSummaryMapper();

  private static final String SELECT_PAGINATED_PAYMENTS_SUMMARIES = "SELECT "
      + "t_payment.payment_id, "
      + "t_payment.total_paid, "
      + "t_payment.user_id as payer_id, "
      + "t_payment.insert_timestamp as payment_date, "
      + "(SELECT COUNT(*) FROM caz_payment.t_clean_air_zone_entrant_payment_match "
      + "WHERE t_payment.payment_id = t_clean_air_zone_entrant_payment_match.payment_id) "
      + "AS entries_count "
      + "FROM caz_payment.t_payment "
      + "WHERE user_id = ANY (?) "
      + "AND payment_provider_status = 'SUCCESS' "
      + "ORDER BY t_payment.insert_timestamp DESC "
      + "LIMIT ? "
      + "OFFSET ?";

  private static final String SELECT_PAYMENT_SUMMARIES_COUNT_FOR_USER_IDS = "SELECT count(*) "
      + "FROM caz_payment.t_payment "
      + "WHERE user_id = ANY (?) "
      + "AND payment_provider_status = 'SUCCESS'";

  /**
   * Method responsible for fetching paginated payments data for the provided users.
   *
   * @param userIds List of the user ids.
   * @param pageNumber page number
   * @param pageSize page size
   * @return List of {@link PaymentSummary}.
   */
  public List<PaymentSummary> getPaginatedPaymentSummaryForUserIds(List<UUID> userIds,
      int pageNumber, int pageSize) {
    Preconditions.checkNotNull(userIds, "userIds cannot be null.");

    return jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          SELECT_PAGINATED_PAYMENTS_SUMMARIES);
      preparedStatement.setArray(1, connection.createArrayOf("uuid", userIds.toArray()));
      preparedStatement.setInt(2, pageSize);
      preparedStatement.setInt(3, pageNumber * pageSize);
      return preparedStatement;
    }, ROW_MAPPER);
  }

  /**
   * Method responsible for getting payments count for the provided users.
   *
   * @param userIds list of the user ids.
   * @return payments count.
   */
  public Integer getTotalPaymentsCountForUserIds(List<UUID> userIds) {
    Preconditions.checkNotNull(userIds, "userIds cannot be null.");

    List<Integer> result = jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          SELECT_PAYMENT_SUMMARIES_COUNT_FOR_USER_IDS);
      preparedStatement.setArray(1, connection.createArrayOf("uuid", userIds.toArray()));
      return preparedStatement;
    }, (resultSet, i) -> resultSet.getInt("count"));

    return result.iterator().next();
  }

  /**
   * A class which maps the results obtained from the database to instances of {@link
   * PaymentSummary} class.
   */
  private static class PaymentSummaryMapper implements RowMapper<PaymentSummary> {

    @Override
    public PaymentSummary mapRow(ResultSet resultSet, int i) throws SQLException {
      return PaymentSummary.builder()
          .paymentId(UUID.fromString(resultSet.getString("payment_id")))
          .totalPaid(resultSet.getInt("total_paid"))
          .payerId(UUID.fromString(resultSet.getString("payer_id")))
          .entriesCount(resultSet.getInt("entries_count"))
          .paymentDate(resultSet.getDate("payment_date").toLocalDate())
          .build();
    }
  }
}
