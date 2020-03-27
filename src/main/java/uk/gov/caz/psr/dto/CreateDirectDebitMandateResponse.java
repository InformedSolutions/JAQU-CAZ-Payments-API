package uk.gov.caz.psr.dto;

import lombok.Builder;
import lombok.Value;

/**
 * A value object that represents the response returned upon the call to create new
 * DirectDebitMandate.
 */
@Value
@Builder
public class CreateDirectDebitMandateResponse {

  /**
   * URL to Payment Provider to create DirectDebitMandate.
   */
  String nextUrl;
}
