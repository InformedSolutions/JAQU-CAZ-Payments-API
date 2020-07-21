package uk.gov.caz.psr.dto.accounts;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for User for Account retrieval response.
 */
@Value
@Builder
public class AccountUsersResponse {

  /**
   * The list of VRNs associated with the account ID provided in the request.
   */
  List<AccountUserResponse> users;
}
