package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

/**
 * Value object representing response returned from vehicle retrieval endpoint.
 */
@Builder
@Value
public class VehicleRetrievalResponseDto {
  
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
   * The total number of vehicles associated with this account.
   */
  long totalVrnsCount;
  
  /**
   * A list of vehicles with their corresponding compliance outcomes
   * from the Compliance Checker service.
   */
  List<ComplianceResultsDto> vehicles;

}
