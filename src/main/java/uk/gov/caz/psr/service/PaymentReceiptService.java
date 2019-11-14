package uk.gov.caz.psr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;

/**
 * Service class to handle SendEmailRequest objects.
 */
@Service
@NoArgsConstructor
public class PaymentReceiptService {

  @Value("${services.sqs.template-id}")
  String templateId;

  /**
   * Creates a SendEmailRequest object.
   * 
   * @param email the recipient of the email
   * @param amount the total cost of their CAZ charge
   * @return SendEmailRequest
   * @throws JsonProcessingException if the amount cannot be serialized into a json string
   */
  public SendEmailRequest buildSendEmailRequest(String email, int amount)
      throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> personalisationMap = new HashMap<String, String>();
    personalisationMap.put("amount", Integer.toString(amount));

    String personalisation = objectMapper.writeValueAsString(personalisationMap);
    return SendEmailRequest.builder().emailAddress(email).personalisation(personalisation)
        .templateId(templateId).build();

  }
}
