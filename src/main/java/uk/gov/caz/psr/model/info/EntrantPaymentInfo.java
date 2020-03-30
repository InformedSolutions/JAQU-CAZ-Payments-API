package uk.gov.caz.psr.model.info;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import uk.gov.caz.psr.model.InternalPaymentStatus;

/**
 * A unique representation of an entrant-payment for the given (vrn, caz, date).
 */
@Entity
@Table(schema = "caz_payment", name = "t_clean_air_zone_entrant_payment")
@Data
public class EntrantPaymentInfo {

  @Id
  @Column(name = "clean_air_zone_entrant_payment_id")
  UUID id;

  @Column(name = "clean_air_zone_id")
  UUID cleanAirZoneId;

  @Column
  String vrn;

  @Column(name = "travel_date")
  LocalDate travelDate;

  @Column(name = "charge")
  Integer chargePaid;

  @Column(name = "payment_status")
  @Enumerated(EnumType.STRING)
  InternalPaymentStatus paymentStatus;

  @Column(name = "case_reference")
  String caseReference;
}
