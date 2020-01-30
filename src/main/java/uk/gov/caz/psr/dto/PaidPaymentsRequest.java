package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.util.MapPreservingOrderBuilder;

/**
 * Class that represents the incoming JSON when client asks for days that are already paid.
 */
@Value
@Builder
public class PaidPaymentsRequest {

  /**
   * First day in date range in which the payments are going to be checked.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-request.start-date}")
  LocalDate startDate;

  /**
   * Last day in date range in which the payments are going to be checked.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-request.end-date}")
  LocalDate endDate;

  /**
   * List of VRNs for which the payments check is going to be done.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-request.vrns}")
  List<String> vrns;

  private static final Map<Function<PaidPaymentsRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<PaidPaymentsRequest, Boolean>, String>builder()
          .put(paidPaymentsRequest -> paidPaymentsRequest.startDate != null,
              "startDate cannot be null.")
          .put(paidPaymentsRequest -> paidPaymentsRequest.endDate != null,
              "endDate cannot be null.")
          .put(paidPaymentsRequest -> paidPaymentsRequest.startDate
                  .isBefore(paidPaymentsRequest.endDate) || paidPaymentsRequest.startDate
                  .isEqual(paidPaymentsRequest.endDate),
              "endDate cannot be before startDate.")
          .put(paidPaymentsRequest -> paidPaymentsRequest.vrns != null,
              "VRNs cannot be blank.")
          .put(paidPaymentsRequest -> !paidPaymentsRequest.vrns.isEmpty(),
              "VRNs cannot be empty.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }
}

