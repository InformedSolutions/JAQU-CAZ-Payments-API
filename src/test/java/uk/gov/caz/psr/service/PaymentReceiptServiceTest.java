package uk.gov.caz.psr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.dto.SendEmailRequest;

public class PaymentReceiptServiceTest {

  private PaymentReceiptService paymentReceiptService;
  private String templateId = "test-template-id";

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
    SendEmailRequest request = paymentReceiptService.buildSendEmailRequest(testEmail, 1.0);

    // then
    assertNotNull(request);
    assertEquals(testEmail, request.getEmailAddress());
    assertEquals("{\"amount\":\"1.00\"}", request.getPersonalisation());
    assertEquals(templateId, request.getTemplateId());
  }

}
