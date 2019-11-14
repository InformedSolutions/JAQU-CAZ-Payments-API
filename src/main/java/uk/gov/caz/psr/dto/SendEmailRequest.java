package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * Domain object representing an email request to be sent to Gov.UK Notify.
 */
@Builder
@Data
public class SendEmailRequest {
  public final String reference = UUID.randomUUID().toString();

  public String templateId;
  public String emailAddress;
  public String personalisation;

}
