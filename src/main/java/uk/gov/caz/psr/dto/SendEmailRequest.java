package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SendEmailRequest {
  public final String reference = UUID.randomUUID().toString();

  public String templateId;
  public String emailAddress;
  public String personalisation;

}
