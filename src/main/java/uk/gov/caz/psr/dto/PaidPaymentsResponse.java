package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.EntrantPayment;

/**
 * Class that represents the returned JSON when client asks for days that are already paid.
 */
@Value
@Builder
public class PaidPaymentsResponse {

  /**
   * Collection of VRNs and dates which are already paid in a given date range.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-response.results}")
  List<PaidPaymentsResult> results;

  /**
   * Converts provided collection to {@link PaidPaymentsResponse}.
   */
  public static PaidPaymentsResponse from(Map<String, List<EntrantPayment>> results) {
    List<PaidPaymentsResult> mappedResults = results.entrySet()
        .stream()
        .map(entry -> buildPaidPaymentResultFrom(entry.getKey(), entry.getValue()))
        .sorted((object1, object2) -> object1.getVrn().compareTo(object2.getVrn()))
        .collect(Collectors.toList());

    return PaidPaymentsResponse.builder().results(mappedResults).build();
  }

  /**
   * Builds {@link PaidPaymentsResult} based on provided VRN and list of {@link EntrantPayment}.
   */
  private static PaidPaymentsResult buildPaidPaymentResultFrom(String vrn,
      List<EntrantPayment> entrantPayments) {
    return PaidPaymentsResult.builder()
        .vrn(vrn)
        .paidDates(collectTravelDates(entrantPayments)).build();
  }

  /**
   * Maps list of {@link EntrantPayment} to list of its travel date.
   */
  private static List<LocalDate> collectTravelDates(List<EntrantPayment> entrantPayments) {
    return entrantPayments
        .stream()
        .map(EntrantPayment::getTravelDate)
        .collect(Collectors.toList());
  }

  /**
   * Class that represents the information about paid dates for a specific VRN and which is returned
   * to User in {@link PaidPaymentsResponse}.
   */
  @Value
  @Builder
  public static class PaidPaymentsResult {

    /**
     * A VRN for which the list of paid days is being returned.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-result.vrn}")
    String vrn;

    /**
     * A list of days which are already paid.
     */
    @ApiModelProperty(value = "${swagger.model.descriptions.paid-payments-result.paid-dates}")
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    List<LocalDate> paidDates;
  }
}
