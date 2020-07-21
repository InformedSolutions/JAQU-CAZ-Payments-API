package uk.gov.caz.psr.model;

import lombok.Builder;
import lombok.Value;

/**
 * Class responsible for storing information about pagination.
 */
@Value
@Builder
public class PaginationData {

  /**
   * Page number.
   */
  int pageNumber;

  /**
   * Size of the page.
   */
  int pageSize;

  /**
   * Quantity of all the elements stored in the data source.
   */
  int totalElementsCount;

  /**
   * Quantity of pages for the provided page size.
   */
  int pageCount;
}
