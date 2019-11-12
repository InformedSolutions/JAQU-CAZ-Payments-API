package uk.gov.caz.psr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailRequest {

  public String templateId;
  public String emailAddress;
  public String personalisation;
  public String reference;

}
