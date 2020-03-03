package uk.gov.caz.psr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;

/**
 * Service class to handle SendEmailRequest objects.
 */
@RequiredArgsConstructor
@Service
public class PaymentReceiptService {
  
  @Value("${services.sqs.template-id}")
  private String singleVehicleTemplateId;
  
  @Value("${services.sqs.account-payment-template-id}")
  private String multipleVehicleTemplateId;
  
  private final ObjectMapper objectMapper;

  /**
   * Creates a SendEmailRequest object.
   *
   * @param email the recipient of the email
   * @param amount the total cost of their CAZ charge
   * @param cazName the name of the Clean Air Zone
   * @param reference the internal payment reference
   * @param vrn the vrn paid for
   * @param externalId the external identifier for the payment
   * @param datesPaidFor the travel dates paid for as an array
   * @return SendEmailRequest
   * @throws JsonProcessingException if the amount cannot be serialized into a json string
   */
  public SendEmailRequest buildSendEmailRequest(String email, double amount, 
      String cazName, String reference, String vrn, String externalId, 
      List<String> datesPaidFor)
      throws JsonProcessingException {
    return SendEmailRequest.builder().emailAddress(email)
        .personalisation(createPersonalisationPayload(amount, cazName, reference, vrn, 
            externalId, datesPaidFor))
        .templateId(this.singleVehicleTemplateId).build();
  }

  /**
   * Builds an email request which supports multiple VRNs.
   * @param emailAddress the email address of the recipient
   * @param totalAmount the total amount paid
   * @param cazName the name of the Clean Air Zone paid for
   * @param referenceNumber the internal reference of the payment
   * @param travelDatesWithVrns a list of formatted strings containing travel date, vrn and charge
   * @param externalId the external identifier of the payment
   * @return SendEmailRequest an instance of {@link SendEmailRequest}
   * @throws JsonProcessingException if the amount cannot be serialized into a json string
   */
  public SendEmailRequest buildSendEmailRequestForMultipleVrns(String emailAddress,
      double totalAmount, String cazName, String referenceNumber, List<String> travelDatesWithVrns,
      String externalId) throws JsonProcessingException {
    // TODO Auto-generated method stub
    return SendEmailRequest.builder().emailAddress(emailAddress)
        .personalisation(createPersonalisationPayloadForMultipleVrns(cazName, 
            travelDatesWithVrns, externalId, referenceNumber, totalAmount))
        .templateId(this.multipleVehicleTemplateId).build();
  }

  private String createPersonalisationPayloadForMultipleVrns(String cazName,
      List<String> chargesPaid, String externalId, String reference, double amount) 
          throws JsonProcessingException {
    Map<String, Object> personalisationMap = new HashMap<String, Object>();    
    personalisationMap.put("caz", cazName);
    personalisationMap.put("charges", chargesPaid);
    personalisationMap.put("external_id", externalId);
    personalisationMap.put("reference", reference);
    personalisationMap.put("amount", String.format(Locale.UK, "%.2f", amount));
    return objectMapper.writeValueAsString(personalisationMap);
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
