package uk.gov.caz.psr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;

/**
 * Entity keeping Entrant Payment historical properties.
 */
@Value
@Builder(toBuilder = true)
public class EntrantPaymentEnriched {

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.travelDate}")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate travelDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentTimestamp}")
  @JsonFormat(pattern = "yyyy-MM-dd'T'hh:mm:ss")
  private LocalDateTime paymentTimestamp;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.operatorId}")
  private Long operatorId;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.cazName}")
  private String cazName;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentId}")
  private UUID paymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentReference}")
  private Long paymentReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentStatus}")
  private ExternalPaymentStatus paymentProviderStatus;

  /**
   * Function constructing entity based on database entities.
   * @param matchInfo entity keeping info on entrant.
   * @param cazIdMap map of CAZ ids and its respective names.
   * @return this entity.
   */
  public static EntrantPaymentEnriched fromMatchInfo(EntrantPaymentMatchInfo matchInfo,
      Map<UUID, String> cazIdMap) {
    Preconditions.checkNotNull(matchInfo, "match object should be not-null");

    return EntrantPaymentEnriched.builder()
        .travelDate(matchInfo.getEntrantPaymentInfo().getTravelDate())
        .paymentTimestamp(matchInfo.getPaymentInfo().getSubmittedTimestamp())
        //.operatorId(matchInfo.getPaymentInfo().operatorid)
        .cazName(cazIdMap.get(matchInfo.getEntrantPaymentInfo().getCleanAirZoneId()))
        .paymentId(matchInfo.getPaymentInfo().getId())
        .paymentReference(matchInfo.getPaymentInfo().getReferenceNumber())
        .paymentProviderStatus(matchInfo.getPaymentInfo().getExternalPaymentStatus())
        .build();
  }

}
