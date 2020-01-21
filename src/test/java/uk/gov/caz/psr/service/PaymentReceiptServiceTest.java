package uk.gov.caz.psr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.mapping.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.dto.SendEmailRequest;

public class PaymentReceiptServiceTest {

  private PaymentReceiptService paymentReceiptService;
  private String templateId = "test-template-id";
  private ObjectMapper om = new ObjectMapper();

  @BeforeEach
  void init() {
    paymentReceiptService = new PaymentReceiptService(templateId, new ObjectMapper());
  }

  @Test
  void canInstantiatePaymentReceiptService() {
    PaymentReceiptService paymentReceiptService =
        new PaymentReceiptService("test", new ObjectMapper());
    assertNotNull(paymentReceiptService);
  }

  @Test
  void canSendPaymentReceipt() throws JsonProcessingException {
    // given
    String testEmail = "test@test.com";

    // when
    SendEmailRequest request = paymentReceiptService.buildSendEmailRequest(
        testEmail, 1.0, "Leeds", "1001", "CAS300", "externalidentifier");

    // then
    assertNotNull(request);
    assertEquals(testEmail, request.getEmailAddress());
    
    HashMap<String, String> personalisation = om.readValue(request.getPersonalisation(), HashMap.class);
    assertTrue(personalisation.containsKey("amount"));
    assertTrue(personalisation.containsKey("date"));
    assertTrue(personalisation.containsKey("reference"));
    assertTrue(personalisation.containsKey("vrn"));
    assertTrue(personalisation.containsKey("caz"));
    assertEquals(templateId, request.getTemplateId());
  }

}
