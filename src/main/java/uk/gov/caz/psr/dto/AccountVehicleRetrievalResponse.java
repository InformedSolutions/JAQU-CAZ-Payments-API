package uk.gov.caz.psr.dto;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for vehicle retrieval response.
 */
@Value
@Builder
public class AccountVehicleRetrievalResponse {

  /**
   * The list of VRNs associated with the account ID provided in the request.
   */
  List<String> vrns;
  
  /**
   * The total number of VRNs that are associated with the account.
   */
  long totalVrnsCount;
  
  /**
   * The total number of pages that can be queried.
   */
  int pageCount;
}
