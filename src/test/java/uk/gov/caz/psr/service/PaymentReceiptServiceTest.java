package uk.gov.caz.psr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;

@ExtendWith(MockitoExtension.class)
public class PaymentReceiptServiceTest {

  @Autowired
  MessagingClient msgClient;

  @InjectMocks
  PaymentReceiptService paymentReceiptService;

  @Mock
  MessagingClient messagingClient;

  @Test
  void canInstantiatePaymentReceiptService() {
    PaymentReceiptService paymentReceiptService = new PaymentReceiptService();
    assertNotNull(paymentReceiptService);
  }

  @Test
  void canSendPaymentReceipt() throws JsonProcessingException {
    String testEmail = "test@test.com";
    String templateId = "test-template-id";
    ReflectionTestUtils.setField(paymentReceiptService, "templateId", templateId);
    SendEmailRequest request = paymentReceiptService.buildSendEmailRequest(testEmail, 1);
    assertNotNull(request);
    assertEquals(testEmail, request.getEmailAddress());
    assertEquals("{\"amount\":\"1\"}", request.getPersonalisation());
    assertEquals(templateId, request.getTemplateId());
  }

}
