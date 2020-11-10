package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.psr.controller.PaymentReceiptController.BASE_PATH;
import static uk.gov.caz.psr.controller.PaymentReceiptController.RESEND_RECEIPT_EMAIL;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.ResendReceiptEmailRequest;
import uk.gov.caz.psr.service.PaymentReceiptService;

@ContextConfiguration(classes = {
    ExceptionController.class,
    Configuration.class,
    PaymentReceiptController.class
})
@WebMvcTest
class PaymentReceiptControllerTest {

  private static final String ANY_REFERENCE_NUMBER = "2307";

  private static final String ANY_CORRELATION_ID = "fb8f036e-eaf0-42e2-bec2-51589d8018ff";

  @MockBean
  private PaymentReceiptService paymentReceiptService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Nested
  class ResendReceiptEmail {

    private static final String RESEND_PATH = BASE_PATH + "/" + RESEND_RECEIPT_EMAIL;

    @Test
    public void shouldReturnStatus400WhenCorrelationIdMissing() throws Exception {
      String payload = "";

      mockMvc.perform(post(RESEND_PATH, ANY_REFERENCE_NUMBER)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));

      verify(paymentReceiptService, never()).sendReceipt(any(), anyString());
    }

    @Test
    public void shouldReturnStatus400WhenEmailValidationFails() throws Exception {
      String payload = createResendReceiptsPayload("");

      mockMvc.perform(post(RESEND_PATH, ANY_REFERENCE_NUMBER)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verify(paymentReceiptService, never()).sendReceipt(any(), anyString());
    }

    @Test
    public void shouldReturnStatus200WhenEmailValidationIsSuccessful() throws Exception {
      String payload = createResendReceiptsPayload("jan@kowalski.com");
      doNothing().when(paymentReceiptService).sendReceipt(any(), anyString());

      mockMvc.perform(post(RESEND_PATH, ANY_REFERENCE_NUMBER)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      verify(paymentReceiptService).sendReceipt(any(), anyString());
    }

    private String createResendReceiptsPayload(String email) {
      ResendReceiptEmailRequest request = ResendReceiptEmailRequest.builder()
          .email(email)
          .build();
      return toJsonString(request);
    }
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
