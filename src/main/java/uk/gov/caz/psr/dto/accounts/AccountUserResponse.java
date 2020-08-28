package uk.gov.caz.psr.dto.accounts;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for response from the Accounts API.
 */
@Value
@Builder
public class AccountUserResponse {

  /**
   * Id of User in DB.
   */
  UUID accountUserId;

  /**
   * Name of the User from the Identity Provider.
   */
  String name;

  /**
   * Email of the User from the Identity Provider.
   */
  String email;

  /**
   * Boolean specifying whether the user is an owner.
   */
  boolean owner;

  /**
   * Boolean specifying whether the user is removed.
   */
  boolean removed;
}
