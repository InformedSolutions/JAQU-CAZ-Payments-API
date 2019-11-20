package uk.gov.caz.psr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.psr.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
public class PaymentReceiptServiceTest {

  PaymentReceiptService paymentReceiptService;
  String templateId = "test-template-id";

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
    String testEmail = "test@test.com";
    ReflectionTestUtils.setField(paymentReceiptService, "templateId", templateId);
    SendEmailRequest request = paymentReceiptService.buildSendEmailRequest(testEmail, 1.0);
    assertNotNull(request);
    assertEquals(testEmail, request.getEmailAddress());
    assertEquals("{\"amount\":\"1.00\"}", request.getPersonalisation());
    assertEquals(templateId, request.getTemplateId());
  }

}
