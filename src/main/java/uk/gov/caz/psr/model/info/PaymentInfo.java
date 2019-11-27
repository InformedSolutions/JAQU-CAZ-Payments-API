package uk.gov.caz.psr.model.info;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import uk.gov.caz.psr.model.ExternalPaymentStatus;

@Entity
@Table(name = "payment")
@Data
public class PaymentInfo {
  @Id
  @Column(name = "payment_id")
  private UUID id;

  @Column(name = "payment_provider_id")
  private String externalId;

  @Column(name = "total_paid")
  private Integer totalPaid;

  @Column(name = "payment_provider_status")
  @Enumerated(EnumType.STRING)
  private ExternalPaymentStatus externalPaymentStatus;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "payment_id")
  private List<VehicleEntrantPaymentInfo> vehicleEntrantPaymentInfoList;
}
