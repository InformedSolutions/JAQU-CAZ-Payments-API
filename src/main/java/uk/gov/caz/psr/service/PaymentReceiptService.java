package uk.gov.caz.psr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;

/**
 * Service class to handle SendEmailRequest objects.
 */
@Service
public class PaymentReceiptService {

  private final String templateId;
  private final ObjectMapper objectMapper;

  public PaymentReceiptService(@Value("${services.sqs.template-id}") String templateId,
      ObjectMapper objectMapper) {
    this.templateId = templateId;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a SendEmailRequest object.
   *
   * @param email the recipient of the email
   * @param amount the total cost of their CAZ charge
   * @return SendEmailRequest
   * @throws JsonProcessingException if the amount cannot be serialized into a json string
   */
  public SendEmailRequest buildSendEmailRequest(String email, double amount, 
      String cazName, String reference, String vrn, String externalId, List<String> datesPaidFor)
      throws JsonProcessingException {
    return SendEmailRequest.builder().emailAddress(email)
        .personalisation(createPersonalisationPayload(amount, cazName, reference, vrn, 
            externalId, datesPaidFor))
        .templateId(templateId).build();
  }

  private String createPersonalisationPayload(double amount, String cazName, 
      String reference, String vrn, String externalId, List<String> datesPaidFor) 
          throws JsonProcessingException {
    Map<String, Object> personalisationMap = new HashMap<String, Object>();    
    personalisationMap.put("amount", String.format(Locale.UK, "%.2f", amount));
    personalisationMap.put("caz", cazName);
    personalisationMap.put("date", datesPaidFor);
    personalisationMap.put("reference", reference);
    personalisationMap.put("vrn", vrn);
    personalisationMap.put("external_id", externalId);
    return objectMapper.writeValueAsString(personalisationMap);
  }
}
