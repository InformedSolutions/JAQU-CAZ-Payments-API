package uk.gov.caz.psr.model.generatecsv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.Data;

/**
 * An entity that represents an EntrantPayment for CSV Export of payments.
 */
@Entity
@Table(schema = "caz_payment", name = "t_clean_air_zone_entrant_payment")
@Data
@IdClass(CsvEntrantPaymentId.class)
public class CsvEntrantPayment {
  @Id
  @Column(name = "clean_air_zone_entrant_payment_id")
  UUID entrantPaymentId;

  @Id
  @Column(name = "clean_air_zone_entrant_payment_match_id")
  UUID entrantPaymentMatchId;

  @Column(name = "payment_id")
  UUID paymentId;

  @Column(name = "payment_submitted_timestamp")
  LocalDateTime dateOfPayment;

  @Column(name = "user_id")
  UUID userId;

  @Column(name = "clean_air_zone_id")
  UUID cleanAirZoneId;

  @Column
  String vrn;

  @Column(name = "travel_date")
  LocalDate travelDate;

  @Column(name = "charge")
  Integer charge;

  @Column(name = "central_reference_number")
  String paymentReference;

  @Column(name = "payment_provider_id")
  String paymentProviderId;

  @Column(name = "total_paid")
  Integer totalPaid;

  @Column(name = "entries_count")
  Integer entriesCount;
}
