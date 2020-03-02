package uk.gov.caz.psr.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Arrays;
import org.hibernate.mapping.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.psr.dto.SendEmailRequest;

public class PaymentReceiptServiceTest {

  private PaymentReceiptService paymentReceiptService;
  private ObjectMapper om = new ObjectMapper();

  @BeforeEach
  void init() {
    paymentReceiptService = new PaymentReceiptService(new ObjectMapper());
  }

  @Test
  void canInstantiatePaymentReceiptService() {
    PaymentReceiptService paymentReceiptService =
        new PaymentReceiptService(new ObjectMapper());
    assertNotNull(paymentReceiptService);
  }

  @Test
  void canSendPaymentReceipt() throws JsonProcessingException {
    // given
    String testEmail = "test@test.com";
    double amount = 1.0;
    String cleanAirZoneName = "Leeds";
    String vrn = "CAS300";
    String reference = "1001";
    String externalId = "externalidentifier";
    ArrayList<String> dates = new ArrayList<String>() {{add("02 February 2020"); add("03 February 2020"); }};
    
    // when
    SendEmailRequest request = paymentReceiptService.buildSendEmailRequest(
        testEmail, amount, cleanAirZoneName, reference, vrn, externalId, dates);

    // then
    assertNotNull(request);
    assertEquals(testEmail, request.getEmailAddress());
    
    HashMap<String, Object> personalisation = om.readValue(request.getPersonalisation(), HashMap.class);
    assertEquals(personalisation.get("amount"), "1.00");
    assertEquals(personalisation.get("date"), dates);
    assertEquals(personalisation.get("reference"), reference);
    assertEquals(personalisation.get("vrn"), vrn);
    assertEquals(personalisation.get("caz"), cleanAirZoneName);
  }

}
