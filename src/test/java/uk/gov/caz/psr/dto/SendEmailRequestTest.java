package uk.gov.caz.psr.dto;

import static org.junit.Assert.assertNotNull;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SendEmailRequestTest {

  @Test
  void canInstantiateSendEmailRequest() throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    Map<String, String> personalisation = new HashMap<String, String>();
    String personalisationStr = om.writeValueAsString(personalisation);
    SendEmailRequest sendEmailRequest = SendEmailRequest.builder().emailAddress("test@test.com")
        .personalisation(personalisationStr).templateId("test-template-id").build();

    assertNotNull(sendEmailRequest);
    assertNotNull(sendEmailRequest.getReference());
  }

}
