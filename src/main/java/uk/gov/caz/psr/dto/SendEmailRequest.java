package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing an email request to be sent to Gov.UK Notify.
 */
@Builder
@Value
public class SendEmailRequest {
  String reference = UUID.randomUUID().toString();
  String templateId;
  String emailAddress;
  String personalisation;
}
