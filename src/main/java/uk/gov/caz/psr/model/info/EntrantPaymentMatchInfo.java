package uk.gov.caz.psr.model.info;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;

/**
 * A link table that matches entrant-payment with the most recent payment.
 */
@Entity
@Table(schema = "caz_payment", name = "t_clean_air_zone_entrant_payment_match")
@Data
public class EntrantPaymentMatchInfo {
  @Id
  UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "clean_air_zone_entrant_payment_id", nullable = false, unique = true)
  EntrantPaymentInfo entrantPaymentInfo;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id", nullable = false, unique = true)
  PaymentInfo paymentInfo;

  @Column
  boolean latest;
}
