package uk.gov.caz.psr.model.info;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import uk.gov.caz.psr.model.InternalPaymentStatus;

@Entity
@Table(name = "vehicle_entrant_payment")
@Data
public class VehicleEntrantPaymentInfo {

  @Id
  @Column(name = "vehicle_entrant_payment_id")
  UUID id;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", nullable = false,
      unique = true)
  PaymentInfo paymentInfo;

  @Column
  String vrn;

  @Column(name = "charge_paid")
  Integer chargePaid;

  @Column(name = "payment_status")
  @Enumerated(EnumType.STRING)
  InternalPaymentStatus paymentStatus;

  @Column(name = "travel_date")
  LocalDate travelDate;

  @Column(name = "case_reference")
  String caseReference;

  @Column(name = "caz_id")
  UUID cleanAirZoneId;
}
