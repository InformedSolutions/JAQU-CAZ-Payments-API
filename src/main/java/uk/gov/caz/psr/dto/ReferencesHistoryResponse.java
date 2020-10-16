package uk.gov.caz.psr.dto;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.util.Strings;

@Value
@Builder
public class ReferencesHistoryResponse {

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history"
      + ".central-reference-number}")
  Long paymentReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.payment-provider-id}")
  String paymentProviderId;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.payment-timestamp}")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime paymentTimestamp;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.total-paid}")
  int totalPaid;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.telephone-payment}")
  boolean telephonePayment;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.operator-id}")
  UUID operatorId;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.caz-name}")
  String cazName;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history"
      + ".payment-provider-status}")
  ExternalPaymentStatus paymentProviderStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.line-items}")
  List<VehicleEntrantPaymentDetails> lineItems;

  @Value
  @Builder
  public static class VehicleEntrantPaymentDetails {

    @ApiModelProperty(value = "${swagger.model.descriptions.references-history.charge-paid}")
    int chargePaid;

    @ApiModelProperty(value = "${swagger.model.descriptions.references-history.travel-date}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate travelDate;

    @ApiModelProperty(value = "${swagger.model.descriptions.references-history.vrn}")
    String vrn;

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @ToString.Include(name = "vrn")
    private String maskedVrn() {
      return Strings.mask(normalizeVrn(vrn));
    }
  }
}