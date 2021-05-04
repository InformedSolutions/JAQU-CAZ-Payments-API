package uk.gov.caz.psr.repository.generatecsv;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.psr.model.generatecsv.CsvEntrantPayment;

/**
 * A class which handles managing data in {@code EntrantPayment} table for CSV Export.
 */
@Repository
public interface CsvEntrantPaymentRepository extends CrudRepository<CsvEntrantPayment, Integer> {

  @Modifying
  @Query(value = "SELECT t_entrant.clean_air_zone_entrant_payment_id, "
      + "t_match.id as clean_air_zone_entrant_payment_match_id, t_payment.payment_id, "
      + "t_payment.user_id, t_payment.payment_submitted_timestamp, t_entrant.clean_air_zone_id, "
      + "t_entrant.vrn, t_entrant.travel_date,t_entrant.charge, t_payment.central_reference_number,"
      + "t_payment.payment_provider_id as payment_provider_id, t_payment.total_paid,"
      + "(SELECT COUNT(*) FROM caz_payment.t_clean_air_zone_entrant_payment_match "
      + "WHERE t_payment.payment_id = t_clean_air_zone_entrant_payment_match.payment_id) "
      + "AS entries_count "
      + "FROM caz_payment.t_clean_air_zone_entrant_payment as t_entrant "
      + "JOIN caz_payment.t_clean_air_zone_entrant_payment_match as t_match ON "
      + "t_match.clean_air_zone_entrant_payment_id = t_entrant.clean_air_zone_entrant_payment_id "
      + "JOIN caz_payment.t_payment as t_payment ON t_payment.payment_id = t_match.payment_id "
      + "WHERE t_payment.user_id in (?1) "
      + "AND t_payment.payment_provider_status = 'SUCCESS'"
      + "ORDER BY t_payment.payment_submitted_timestamp DESC, t_entrant.vrn ASC, "
      + "t_entrant.travel_date ASC",
      nativeQuery = true)
  List<CsvEntrantPayment> findAllForAccountUsers(List<UUID> accountUserIds);
}
