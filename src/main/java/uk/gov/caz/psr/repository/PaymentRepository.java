package uk.gov.caz.psr.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.Payment;

/**
 * A class which handles managing data in {@code PAYMENT} table.
 */
@Repository
@AllArgsConstructor
public class PaymentRepository {

  private final JdbcTemplate jdbcTemplate;

  static final String INSERT_SQL = "insert into PAYMENT (payment_id, external_payment_id, status, "
      + "payment_method, charge_paid, caz_id, correlation_id) values (?, ?, ?, ?, ?, ?, ?)";
  static final String UPDATE_SQL = "UPDATE PAYMENT "
      + "SET status = ?, external_payment_id = ?, update_timestamp = CURRENT_TIMESTAMP"
      + "WHERE payment_id = ?";


  /**
   * Inserts {@code payment} into database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   */
  public void insert(Payment payment) {
    jdbcTemplate.update(INSERT_SQL, payment.getId(), payment.getExternalPaymentId(),
        "initiated", "credit_card", payment.getChargePaid(), payment.getCleanZoneId(),
        payment.getCorrelationId());
  }

  /**
   * Update {@code payment} in the database.
   *
   * @param payment An entity object which is supposed to be saved in the database.
   */
  public void update(Payment payment) {
    jdbcTemplate
        .update(UPDATE_SQL, payment.getStatus(), payment.getExternalPaymentId(), payment.getId());
  }
}

