package uk.gov.caz.psr.dto.accounts;

import java.util.UUID;
import lombok.Value;

@Value
public class UserDetailsResponse {

  /**
   * Id of the account for this user.
   */
  UUID accountId;

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