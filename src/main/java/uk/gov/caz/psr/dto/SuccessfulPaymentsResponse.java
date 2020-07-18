package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;

/**
 * A class that represents a response with payments data of a specific user.
 */
@Value
@Builder
public class SuccessfulPaymentsResponse {

  /**
   * Page that has been retrieved.
   */
  int page;

  /**
   * Total number of pages available (with current page size).
   */
  int pageCount;

  /**
   * The current page size.
   */
  int perPage;

  /**
   * The total number of successful payments associated with the account.
   */
  int totalPaymentsCount;

  /**
   * A list of payments data.
   */
  List<EnrichedPaymentSummary> payments;

  /**
   * Helper method which composes response object from the provided {@link PaginationData} and list
   * of {@link EnrichedPaymentSummary}.
   */
  public static SuccessfulPaymentsResponse from(PaginationData paginationData,
      List<EnrichedPaymentSummary> payments) {
    return SuccessfulPaymentsResponse.builder()
        .perPage(paginationData.getPageSize())
        .totalPaymentsCount(paginationData.getTotalElementsCount())
        .page(paginationData.getPageNumber())
        .pageCount(paginationData.getPageCount())
        .payments(payments)
        .build();
  }
}
