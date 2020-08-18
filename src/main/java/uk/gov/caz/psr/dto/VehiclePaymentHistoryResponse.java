package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;

/**
 * A class that represents a request for vehicle historical payments.
 */
@Value
@Builder
public class VehiclePaymentHistoryResponse {

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payments-response.page}")
  int page;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payments-response.pageCount}")
  int pageCount;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payments-response.perPage}")
  int perPage;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.historical-payments-response.totalPaymentsCount}")
  long totalPaymentsCount;

  @ApiModelProperty(value = "${swagger.model.descriptions.historical-payments-response.payments}")
  List<EntrantPaymentEnriched> payments;

  /**
   * Creates response object from a page returned from repository.
   * @param entrantsPage page of items
   * @return response object
   */
  public static VehiclePaymentHistoryResponse fromPage(Page<EntrantPaymentEnriched> entrantsPage) {
    return VehiclePaymentHistoryResponse.builder()
        .page(entrantsPage.getNumber())
        .pageCount(entrantsPage.getTotalPages())
        .perPage(entrantsPage.getSize())
        .totalPaymentsCount(entrantsPage.getTotalElements())
        .payments(entrantsPage.getContent())
        .build();
  }
}
