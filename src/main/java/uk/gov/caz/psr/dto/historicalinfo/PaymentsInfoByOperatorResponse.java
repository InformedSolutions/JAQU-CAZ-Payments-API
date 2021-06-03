package uk.gov.caz.psr.dto.historicalinfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that represents the response with the paginated information of payments made by the
 * given operator.
 */
@Value
@Builder
public class PaymentsInfoByOperatorResponse {

  /**
   * Current page number.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.payments-info-by-operator-id-response.page}")
  int page;

  /**
   * Total pages count.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.payments-info-by-operator-id-response.page-count}")
  int pageCount;

  /**
   * Requested page size or 10 if not provided.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.payments-info-by-operator-id-response.per-page}")
  int perPage;

  /**
   * Total paginated items count.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.payments-info-by-operator-id-response.total-payments-count}")
  long totalPaymentsCount;

  /**
   * List of items on this page.
   */
  @ApiModelProperty(value =
      "${swagger.model.descriptions.payments-info-by-operator-id-response.payments}")
  List<SinglePaymentsInfoByOperator> payments;

  @Value
  @Builder
  @JsonIgnoreProperties({"modified"})
  public static class SinglePaymentsInfoByOperator {

    /**
     * Datetime that tells when the payment was inserted into the database.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.payment-timestamp}")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime paymentTimestamp;

    /**
     * Clean Air Zone name.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.caz-name}")
    String cazName;

    /**
     * The amount of money paid to enter the CAZ.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.total-paid}")
    int totalPaid;

    /**
     * Internal identifier of this payment.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.payment-id}")
    UUID paymentId;

    /**
     * An identifier of the operator who is making the payment.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.operator-id}")
    UUID operatorId;

    /**
     * Customer-friendly payment reference number.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.payment-reference}")
    long paymentReference;

    /**
     * Collection of VRNs against which the payment was made.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.vrns}")
    Set<String> vrns;

    /**
     * External status of this payment.
     */
    @ApiModelProperty(value =
        "${swagger.model.descriptions.payments-info-by-operator-id-response.status}")
    String paymentProviderStatus;

    /**
     * Flag explaining if any entrant was failed, charged back or refunded.
     */
    @ApiModelProperty(value = "${swagger.model.payments-info-by-operator-id-response.isModified")
    @JsonProperty(value = "isModified")
    boolean isModified;

  }
}
