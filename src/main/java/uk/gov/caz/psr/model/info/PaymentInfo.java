package uk.gov.caz.psr.model.info;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentMethod;

/**
 * An entity that represents a payment made in GOV UK Pay service.
 */
@Entity
@Table(schema = "caz_payment", name = "t_payment")
@Data
public class PaymentInfo {

  @Id
  @Column(name = "payment_id")
  private UUID id;

  @Column(name = "payment_provider_id")
  private String externalId;

  @Column(name = "total_paid")
  private Integer totalPaid;

  @Column(name = "payment_method")
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  @Column(name = "payment_provider_status")
  @Enumerated(EnumType.STRING)
  private ExternalPaymentStatus externalPaymentStatus;

  @Column(name = "central_reference_number")
  private Long referenceNumber;

  @Column(name = "payment_submitted_timestamp")
  private LocalDateTime submittedTimestamp;

  @Column(name = "payment_provider_mandate_id")
  private String paymentProviderMandateId;
}
