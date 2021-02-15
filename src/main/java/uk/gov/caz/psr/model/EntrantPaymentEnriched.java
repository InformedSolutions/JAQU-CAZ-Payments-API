package uk.gov.caz.psr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;

/**
 * Entity keeping Entrant Payment historical properties.
 */
@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties({"refunded", "chargedback"})
public class EntrantPaymentEnriched {

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.travelDate}")
  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate travelDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentTimestamp}")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  LocalDateTime paymentTimestamp;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.operatorId}")
  UUID operatorId;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.cazName}")
  String cazName;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentId}")
  UUID paymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentReference}")
  Long paymentReference;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.paymentStatus}")
  ExternalPaymentStatus paymentProviderStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.isRefunded")
  @JsonProperty(value = "isRefunded")
  boolean isRefunded;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payment.isChargedback")
  @JsonProperty(value = "isChargedback")
  boolean isChargedback;

  /**
   * Function constructing entity based on database entities.
   *
   * @param matchInfo entity keeping info on entrant.
   * @param cazIdMap map of CAZ ids and its respective names.
   * @return this entity.
   */
  public static EntrantPaymentEnriched fromMatchInfo(EntrantPaymentMatchInfo matchInfo,
      Map<UUID, String> cazIdMap, List<PaymentStatusAuditData> auditData) {
    Preconditions.checkNotNull(matchInfo, "match object should be not-null");

    return EntrantPaymentEnriched.builder()
        .travelDate(matchInfo.getEntrantPaymentInfo().getTravelDate())
        .paymentTimestamp(matchInfo.getPaymentInfo().getSubmittedTimestamp())
        .operatorId(matchInfo.getPaymentInfo().getOperatorId())
        .cazName(cazIdMap.get(matchInfo.getEntrantPaymentInfo().getCleanAirZoneId()))
        .paymentId(matchInfo.getPaymentInfo().getId())
        .paymentReference(matchInfo.getPaymentInfo().getReferenceNumber())
        .paymentProviderStatus(matchInfo.getPaymentInfo().getExternalPaymentStatus())
        .isRefunded(isEntrantPaymentRefunded(matchInfo, auditData))
        .isChargedback(isEntrantPaymentChargedBack(matchInfo, auditData))
        .build();
  }

  /**
   * Returns information if a given entrantPayment was charged back.
   */
  private static boolean isEntrantPaymentChargedBack(EntrantPaymentMatchInfo matchInfo,
      List<PaymentStatusAuditData> auditData) {
    return getFilteredAuditData(matchInfo, auditData)
        .anyMatch(e -> e.getPaymentStatus().equals(InternalPaymentStatus.CHARGEBACK));
  }

  /**
   * Returns information if a given entrantPayment was refunded.
   */
  private static boolean isEntrantPaymentRefunded(EntrantPaymentMatchInfo matchInfo,
      List<PaymentStatusAuditData> auditData) {
    return getFilteredAuditData(matchInfo, auditData)
        .anyMatch(e -> e.getPaymentStatus().equals(InternalPaymentStatus.REFUNDED));
  }

  /**
   * Method performs a filtering on the provided {@code List<PaymentStatusAuditData} auditData using
   * the {@link EntrantPaymentMatchInfo}.
   */
  private static Stream<PaymentStatusAuditData> getFilteredAuditData(
      EntrantPaymentMatchInfo matchInfo, List<PaymentStatusAuditData> auditData) {
    return auditData
        .stream()
        .filter(e ->
            e.getCleanAirZoneId().equals(matchInfo.getEntrantPaymentInfo().getCleanAirZoneId())
                && e.getPaymentId().equals(matchInfo.getPaymentInfo().getId())
                && e.getTravelDate().equals(matchInfo.getEntrantPaymentInfo().getTravelDate()));
  }
}
